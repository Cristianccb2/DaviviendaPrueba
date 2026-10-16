package com.prueba.polizas.controller;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad: header x-api-key obligatorio en todos los endpoints. */
class SeguridadApiTest extends ApiTestBase {

    @Test
    void apiKeyAusente_rechaza() throws Exception {
        mockMvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("API_KEY_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value("API key faltante o incorrecta"));
    }

    @Test
    void apiKeyIncorrecta_rechaza() throws Exception {
        mockMvc.perform(get("/polizas").header("x-api-key", "654321"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("API_KEY_INVALIDA"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "GET,  /polizas",
            "GET,  /polizas/1/riesgos",
            "POST, /polizas/1/renovar",
            "POST, /polizas/1/cancelar",
            "POST, /polizas/2/riesgos",
            "POST, /riesgos/1/cancelar",
            "POST, /core-mock/evento"
    })
    void apiKeyAusente_rechazaEnLosSieteEndpoints(String metodo, String ruta) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("API_KEY_INVALIDA"));
    }

    @Test
    void apiKeyAusente_noEjecutaLaOperacion() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);

        mockMvc.perform(post("/polizas/{id}/cancelar", poliza.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(recargar(poliza).getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        verifyNoInteractions(coreClient);
    }
}
