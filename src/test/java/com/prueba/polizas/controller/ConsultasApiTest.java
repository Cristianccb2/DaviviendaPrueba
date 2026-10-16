package com.prueba.polizas.controller;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.model.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Consultas: GET /polizas y GET /polizas/{id}/riesgos. */
class ConsultasApiTest extends ApiTestBase {

    @Test
    void listarPolizas_filtraTipoYEstado() throws Exception {
        crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);
        crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.CANCELADA);
        crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA);

        mockMvc.perform(conApiKey(get("/polizas").param("tipo", "COLECTIVA").param("estado", "ACTIVA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].tipo", everyItem(is("COLECTIVA"))))
                .andExpect(jsonPath("$[*].estado", everyItem(is("ACTIVA"))));

        verifyNoInteractions(coreClient);
    }

    @Test
    void listarPolizas_sinFiltros_devuelveTodas() throws Exception {
        crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.RENOVADA);
        long total = polizaRepository.count();

        mockMvc.perform(conApiKey(get("/polizas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(total));
    }

    @Test
    void listarPolizas_soloPorTipo() throws Exception {
        crearPoliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA);

        mockMvc.perform(conApiKey(get("/polizas").param("tipo", "INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].tipo", everyItem(is("INDIVIDUAL"))));
    }

    @Test
    void listarPolizas_enumInexistente_rechaza() throws Exception {
        mockMvc.perform(conApiKey(get("/polizas").param("estado", "VENCIDA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void listarRiesgos_devuelveRiesgosDeLaPolizaIncluyendoCancelados() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);
        Riesgo activo = crearRiesgo(poliza, EstadoRiesgo.ACTIVO);
        Riesgo cancelado = crearRiesgo(poliza, EstadoRiesgo.CANCELADO);

        mockMvc.perform(conApiKey(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(activo.getId()))
                .andExpect(jsonPath("$[0].polizaId").value(poliza.getId()))
                .andExpect(jsonPath("$[0].descripcion").value("Riesgo de prueba"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$[1].id").value(cancelado.getId()))
                .andExpect(jsonPath("$[1].estado").value("CANCELADO"));

        verifyNoInteractions(coreClient);
    }

    @Test
    void listarRiesgos_polizaSinRiesgos_devuelveArregloVacio() throws Exception {
        Poliza poliza = crearPoliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);

        mockMvc.perform(conApiKey(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void listarRiesgos_polizaInexistente() throws Exception {
        mockMvc.perform(conApiKey(get("/polizas/{id}/riesgos", ID_INEXISTENTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("POLIZA_NO_ENCONTRADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void listarRiesgos_idInvalido_rechaza(String id) throws Exception {
        mockMvc.perform(conApiKey(get("/polizas/" + id + "/riesgos")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }
}
