package com.prueba.polizas.controller;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.model.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alta de riesgos: POST /polizas/{id}/riesgos. */
class RiesgosApiTest extends ApiTestBase {

    @Test
    void agregarRiesgo_colectivaActiva_creaRiesgoActivo() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);

        agregarRiesgo(poliza.getId(), "{\"descripcion\":\"Apartamento 301\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.polizaId").value(poliza.getId()))
                .andExpect(jsonPath("$.descripcion").value("Apartamento 301"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        List<Riesgo> riesgos = riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId());
        assertThat(riesgos).hasSize(1);
        assertThat(riesgos.get(0).getDescripcion()).isEqualTo("Apartamento 301");
        assertThat(riesgos.get(0).getEstado()).isEqualTo(EstadoRiesgo.ACTIVO);
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void agregarRiesgo_colectivaRenovada_creaRiesgo() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.RENOVADA);

        agregarRiesgo(poliza.getId(), "{\"descripcion\":\"Local 2\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void agregarRiesgo_individual_rechaza() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA);
        crearRiesgo(poliza, EstadoRiesgo.ACTIVO);

        agregarRiesgo(poliza.getId(), "{\"descripcion\":\"Segundo riesgo\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TIPO_POLIZA_NO_PERMITE_RIESGOS"));

        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId())).hasSize(1);
        verifyNoInteractions(coreClient);
    }

    @Test
    void agregarRiesgo_colectivaCancelada_rechaza() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.CANCELADA);

        agregarRiesgo(poliza.getId(), "{\"descripcion\":\"Apartamento 301\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("POLIZA_CANCELADA"));

        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId())).isEmpty();
        verifyNoInteractions(coreClient);
    }

    @Test
    void agregarRiesgo_polizaInexistente() throws Exception {
        agregarRiesgo(ID_INEXISTENTE, "{\"descripcion\":\"Apartamento 301\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("POLIZA_NO_ENCONTRADA"));
    }

    @Test
    void agregarRiesgo_descripcionDe255Caracteres_esValida() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);

        agregarRiesgo(poliza.getId(), "{\"descripcion\":\"" + "a".repeat(255) + "\"}")
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void agregarRiesgo_descripcionInvalida_rechaza(String cuerpo) throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);

        agregarRiesgo(poliza.getId(), cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));

        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(poliza.getId())).isEmpty();
        verifyNoInteractions(coreClient);
    }

    static Stream<String> cuerposInvalidos() {
        return Stream.of(
                "{}",
                "{\"descripcion\":null}",
                "{\"descripcion\":\"\"}",
                "{\"descripcion\":\"   \"}",
                "{\"descripcion\":\"" + "a".repeat(256) + "\"}");
    }

    private ResultActions agregarRiesgo(Long polizaId, String cuerpo) throws Exception {
        return mockMvc.perform(conApiKey(post("/polizas/{id}/riesgos", polizaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)));
    }
}
