package com.prueba.polizas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Cliente saliente hacia el CORE: contenido de la petición, envío tras commit y tolerancia a fallos. */
@ExtendWith(OutputCaptureExtension.class)
class CoreClientTest {

    private static final String BASE_URL = "http://core.local";
    private static final String URL_EVENTO = BASE_URL + "/core-mock/evento";

    private MockRestServiceServer servidorCore;
    private CoreClient coreClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidorCore = MockRestServiceServer.bindTo(builder).build();
        coreClient = new CoreClient(builder, BASE_URL, "123456");
    }

    @Test
    void notificar_enviaPostConApiKeyYEvento() {
        servidorCore.expect(requestTo(URL_EVENTO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "123456"))
                .andExpect(content().json("{\"evento\":\"ACTUALIZACION\",\"polizaId\":7}"))
                .andRespond(withSuccess("{\"mensaje\":\"Evento registrado\"}", MediaType.APPLICATION_JSON));

        coreClient.notificarActualizacion(7L);

        servidorCore.verify();
    }

    @Test
    void notificar_conTransaccionActiva_enviaSoloTrasElCommit() {
        servidorCore.expect(requestTo(URL_EVENTO)).andRespond(withSuccess());

        TransactionSynchronizationManager.initSynchronization();
        try {
            coreClient.notificarActualizacion(7L);
            // Antes del commit todavía no se ha enviado nada
            assertThatThrownBy(servidorCore::verify).isInstanceOf(AssertionError.class);

            TransactionSynchronizationUtils.triggerAfterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        servidorCore.verify();
    }

    @Test
    void notificar_falloDelCore_registraWarnSinPropagarError(CapturedOutput output) {
        servidorCore.expect(requestTo(URL_EVENTO)).andRespond(withException(new IOException("Connection refused")));

        assertThatCode(() -> coreClient.notificarActualizacion(7L)).doesNotThrowAnyException();

        assertThat(output).contains("WARN")
                .contains("CORE_CLIENT resultado=FALLO evento=ACTUALIZACION polizaId=7")
                .doesNotContain("123456");
    }
}
