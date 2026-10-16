package com.prueba.polizas;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración real de extremo a extremo: levanta Tomcat en un puerto libre y comprueba que una mutación
 * llama por HTTP al propio endpoint /core-mock/evento (con API key) y que ambos lados quedan en el log.
 * Usa su propia BD en memoria para no interferir con el contexto de las pruebas MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:polizasdb-integracion")
@ExtendWith(OutputCaptureExtension.class)
class IntegracionCoreTest {

    private static final int PUERTO = puertoLibre();

    @DynamicPropertySource
    static void configurarPuerto(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PUERTO);
    }

    @Autowired
    private PolizaRepository polizaRepository;

    @Test
    void cancelarPoliza_enviaEventoAlCoreMockPorHttp(CapturedOutput output) {
        Poliza poliza = polizaRepository.save(new Poliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA,
                new BigDecimal("1000.00"), new BigDecimal("12000.00")));

        ResponseEntity<String> respuesta = RestClient.create("http://localhost:" + PUERTO)
                .post()
                .uri("/polizas/{id}/cancelar", poliza.getId())
                .header("x-api-key", "123456")
                .retrieve()
                .toEntity(String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(output)
                .contains("CORE_CLIENT intento evento=ACTUALIZACION polizaId=" + poliza.getId())
                .contains("CORE_MOCK intento evento=ACTUALIZACION polizaId=" + poliza.getId())
                .contains("CORE_CLIENT resultado=OK evento=ACTUALIZACION polizaId=" + poliza.getId());
    }

    private static int puertoLibre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
