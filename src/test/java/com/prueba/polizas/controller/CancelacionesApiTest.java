package com.prueba.polizas.controller;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.model.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cancelación de pólizas (POST /polizas/{id}/cancelar) y de riesgos (POST /riesgos/{id}/cancelar). */
class CancelacionesApiTest extends ApiTestBase {

    @Test
    void cancelarPoliza_cancelaTodosSusRiesgos() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);
        Riesgo riesgo1 = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);
        Riesgo riesgo2 = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        cancelarPoliza(poliza.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(poliza.getId()))
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertThat(recargar(poliza).getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(recargar(riesgo1).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
        assertThat(recargar(riesgo2).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void cancelarPoliza_renovada_seCancela() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.RENOVADA);
        Riesgo riesgo = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        cancelarPoliza(poliza.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertThat(recargar(riesgo).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
    }

    @Test
    void cancelarPoliza_repetida_noCausaCambiosAdicionales() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);
        crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        cancelarPoliza(poliza.getId()).andExpect(status().isOk());
        cancelarPoliza(poliza.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertThat(recargar(poliza).getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        // Solo la primera cancelación es un cambio efectivo
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void cancelarPoliza_inexistente() throws Exception {
        cancelarPoliza(ID_INEXISTENTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("POLIZA_NO_ENCONTRADA"));
    }

    @Test
    void cancelarPoliza_idNoNumerico_rechaza() throws Exception {
        mockMvc.perform(conApiKey(post("/polizas/abc/cancelar")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void cancelarRiesgo_noCancelaPoliza() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);
        Riesgo aCancelar = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);
        Riesgo otro = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        cancelarRiesgo(aCancelar.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aCancelar.getId()))
                .andExpect(jsonPath("$.polizaId").value(poliza.getId()))
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        assertThat(recargar(aCancelar).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
        assertThat(recargar(otro).getEstado()).isEqualTo(EstadoRiesgo.ACTIVO);
        assertThat(recargar(poliza).getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void cancelarRiesgo_repetido_noAlteraEstado() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);
        Riesgo riesgo = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        cancelarRiesgo(riesgo.getId()).andExpect(status().isOk());
        cancelarRiesgo(riesgo.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        assertThat(recargar(riesgo).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void cancelarRiesgo_unicoDeIndividual_dejaPolizaVigente() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA);
        Riesgo unico = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        cancelarRiesgo(unico.getId()).andExpect(status().isOk());

        assertThat(recargar(unico).getEstado()).isEqualTo(EstadoRiesgo.CANCELADO);
        assertThat(recargar(poliza).getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId())).hasSize(1);
    }

    @Test
    void cancelarRiesgo_inexistente() throws Exception {
        cancelarRiesgo(ID_INEXISTENTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RIESGO_NO_ENCONTRADO"));
    }

    private ResultActions cancelarPoliza(Long id) throws Exception {
        return mockMvc.perform(conApiKey(post("/polizas/{id}/cancelar", id)));
    }

    private ResultActions cancelarRiesgo(Long id) throws Exception {
        return mockMvc.perform(conApiKey(post("/riesgos/{id}/cancelar", id)));
    }
}
