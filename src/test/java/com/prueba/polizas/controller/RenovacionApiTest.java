package com.prueba.polizas.controller;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renovación: POST /polizas/{id}/renovar. */
class RenovacionApiTest extends ApiTestBase {

    @Test
    void renovar_activa_aplicaIPC() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA, "1000.00", "12000.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(poliza.getId()))
                .andExpect(jsonPath("$.estado").value("RENOVADA"))
                .andExpect(content().string(containsString("\"canonMensual\":1050.00")))
                .andExpect(content().string(containsString("\"prima\":12600.00")));

        Poliza renovada = recargar(poliza);
        assertThat(renovada.getCanonMensual()).isEqualTo(new BigDecimal("1050.00"));
        assertThat(renovada.getPrima()).isEqualTo(new BigDecimal("12600.00"));
        assertThat(renovada.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void renovar_redondeaHalfUpADosDecimales() throws Exception {
        // 10.10 * 1.05 = 10.605 -> HALF_UP -> 10.61
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA, "10.10", "0.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":5}").andExpect(status().isOk());

        Poliza renovada = recargar(poliza);
        assertThat(renovada.getCanonMensual()).isEqualTo(new BigDecimal("10.61"));
        assertThat(renovada.getPrima()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void renovar_ipcDecimal_aplicaPorcentaje() throws Exception {
        // 1000.00 * 1.052 = 1052.00 ; 12000.00 * 1.052 = 12624.00
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA, "1000.00", "12000.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":5.20}").andExpect(status().isOk());

        Poliza renovada = recargar(poliza);
        assertThat(renovada.getCanonMensual()).isEqualTo(new BigDecimal("1052.00"));
        assertThat(renovada.getPrima()).isEqualTo(new BigDecimal("12624.00"));
    }

    @Test
    void renovar_renovada_aplicaIPCSobreImportesActuales() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.RENOVADA, "1050.00", "12600.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":5}").andExpect(status().isOk());

        Poliza renovada = recargar(poliza);
        assertThat(renovada.getCanonMensual()).isEqualTo(new BigDecimal("1102.50"));
        assertThat(renovada.getPrima()).isEqualTo(new BigDecimal("13230.00"));
        assertThat(renovada.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
    }

    @Test
    void renovar_ipcCien_duplicaImportes() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA, "1000.00", "12000.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":100}").andExpect(status().isOk());

        assertThat(recargar(poliza).getCanonMensual()).isEqualTo(new BigDecimal("2000.00"));
    }

    @Test
    void renovar_cancelada_rechaza() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.CANCELADA, "1000.00", "12000.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":5}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("POLIZA_CANCELADA"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        Poliza sinCambios = recargar(poliza);
        assertThat(sinCambios.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(sinCambios.getCanonMensual()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(sinCambios.getPrima()).isEqualTo(new BigDecimal("12000.00"));
        verifyNoInteractions(coreClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"ipcPorcentaje\":null}",
            "{\"ipcPorcentaje\":-1}",
            "{\"ipcPorcentaje\":100.01}",
            "{\"ipcPorcentaje\":\"abc\"}",
            "{\"ipcPorcentaje\":\"5\"}",
            "{\"ipcPorcentaje\":true}",
            "no-es-json"
    })
    void renovar_ipcInvalido_rechaza(String cuerpo) throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA, "1000.00", "12000.00");

        renovar(poliza.getId(), cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));

        assertThat(recargar(poliza).getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        verifyNoInteractions(coreClient);
    }

    @Test
    void renovar_sinCuerpo_rechaza() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA);

        mockMvc.perform(conApiKey(post("/polizas/{id}/renovar", poliza.getId()).contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void renovar_polizaInexistente() throws Exception {
        renovar(ID_INEXISTENTE, "{\"ipcPorcentaje\":5}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("POLIZA_NO_ENCONTRADA"));
    }

    @Test
    void renovar_idNoPositivo_rechaza() throws Exception {
        renovar(0L, "{\"ipcPorcentaje\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void renovar_ipcCero_activa_pasaARenovadaSinCambiarImportes() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA, "1000.00", "12000.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));

        Poliza renovada = recargar(poliza);
        assertThat(renovada.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(renovada.getCanonMensual()).isEqualTo(new BigDecimal("1000.00"));
        verify(coreClient, times(1)).notificarActualizacion(poliza.getId());
    }

    @Test
    void renovar_ipcCero_renovada_noHayCambio() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.RENOVADA, "1000.00", "12000.00");

        renovar(poliza.getId(), "{\"ipcPorcentaje\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));

        assertThat(recargar(poliza).getCanonMensual()).isEqualTo(new BigDecimal("1000.00"));
        verifyNoInteractions(coreClient);
    }

    private ResultActions renovar(Long id, String cuerpo) throws Exception {
        return mockMvc.perform(conApiKey(post("/polizas/{id}/renovar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)));
    }
}
