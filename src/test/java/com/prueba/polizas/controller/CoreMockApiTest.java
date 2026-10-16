package com.prueba.polizas.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORE simulado: POST /core-mock/evento. */
@ExtendWith(OutputCaptureExtension.class)
class CoreMockApiTest extends ApiTestBase {

    @Test
    void coreMock_logueaEvento(CapturedOutput output) throws Exception {
        long polizasAntes = polizaRepository.count();
        long riesgosAntes = riesgoRepository.count();

        // La póliza 555 no existe: el mock no la valida ni modifica la BD
        registrarEvento("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Evento registrado"));

        assertThat(output).contains("CORE_MOCK intento evento=ACTUALIZACION polizaId=555");
        assertThat(polizaRepository.count()).isEqualTo(polizasAntes);
        assertThat(riesgoRepository.count()).isEqualTo(riesgosAntes);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"evento\":\"BORRADO\",\"polizaId\":555}",
            "{\"polizaId\":555}",
            "{\"evento\":\"ACTUALIZACION\"}",
            "{\"evento\":\"ACTUALIZACION\",\"polizaId\":0}",
            "{\"evento\":\"ACTUALIZACION\",\"polizaId\":-5}",
            "{\"evento\":\"ACTUALIZACION\",\"polizaId\":5.5}",
            "{\"evento\":\"ACTUALIZACION\",\"polizaId\":\"abc\"}"
    })
    void coreMock_eventoInvalido_rechaza(String cuerpo, CapturedOutput output) throws Exception {
        registrarEvento(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));

        assertThat(output).doesNotContain("CORE_MOCK intento");
    }

    private ResultActions registrarEvento(String cuerpo) throws Exception {
        return mockMvc.perform(conApiKey(post("/core-mock/evento")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)));
    }
}
