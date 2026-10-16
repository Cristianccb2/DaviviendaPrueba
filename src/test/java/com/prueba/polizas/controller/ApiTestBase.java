package com.prueba.polizas.controller;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import com.prueba.polizas.repository.RiesgoRepository;
import com.prueba.polizas.service.CoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

/**
 * Base de las pruebas de API: contexto completo, H2 en memoria y MockMvc.
 * Cada prueba crea sus propios datos para no depender de las semillas ni del orden de ejecución.
 * MockMvc no levanta servidor HTTP, por eso el cliente del CORE se sustituye por un mock
 * para verificar cuándo se emite (o no) el evento; la llamada HTTP real se prueba en IntegracionCoreTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestBase {

    protected static final long ID_INEXISTENTE = 999_999L;

    @MockitoBean
    protected CoreClient coreClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PolizaRepository polizaRepository;

    @Autowired
    protected RiesgoRepository riesgoRepository;

    protected MockHttpServletRequestBuilder conApiKey(MockHttpServletRequestBuilder request) {
        return request.header("x-api-key", "123456");
    }

    protected Poliza crearPoliza(TipoPoliza tipo, EstadoPoliza estado) {
        return crearPoliza(tipo, estado, "1000.00", "12000.00");
    }

    protected Poliza crearPoliza(TipoPoliza tipo, EstadoPoliza estado, String canonMensual, String prima) {
        return polizaRepository.save(new Poliza(tipo, estado, new BigDecimal(canonMensual), new BigDecimal(prima)));
    }

    protected Riesgo crearRiesgo(Poliza poliza, EstadoRiesgo estado) {
        return riesgoRepository.save(new Riesgo(poliza, "Riesgo de prueba", estado));
    }

    protected Poliza recargar(Poliza poliza) {
        return polizaRepository.findById(poliza.getId()).orElseThrow();
    }

    protected Riesgo recargar(Riesgo riesgo) {
        return riesgoRepository.findById(riesgo.getId()).orElseThrow();
    }
}
