package com.prueba.polizas.repository;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica el modelo, los datos semilla y la consulta con filtros opcionales. */
@DataJpaTest
class PolizaRepositoryTest {

    @Autowired
    private PolizaRepository polizaRepository;

    @Autowired
    private RiesgoRepository riesgoRepository;

    @Test
    void datosSemilla_estanCargados() {
        Poliza individual = polizaRepository.findById(1L).orElseThrow();
        assertThat(individual.getTipo()).isEqualTo(TipoPoliza.INDIVIDUAL);
        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(1L)).hasSize(1);

        Poliza colectiva = polizaRepository.findById(2L).orElseThrow();
        assertThat(colectiva.getTipo()).isEqualTo(TipoPoliza.COLECTIVA);
        assertThat(riesgoRepository.findByPolizaIdOrderByIdAsc(2L)).isNotEmpty();

        assertThat(polizaRepository.findById(3L).orElseThrow().getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
    }

    @Test
    void buscar_sinFiltros_devuelveTodas() {
        assertThat(polizaRepository.buscar(null, null)).hasSize((int) polizaRepository.count());
    }

    @Test
    void buscar_combinaFiltrosConAnd() {
        List<Poliza> resultado = polizaRepository.buscar(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA);

        assertThat(resultado).isNotEmpty().allSatisfy(poliza -> {
            assertThat(poliza.getTipo()).isEqualTo(TipoPoliza.COLECTIVA);
            assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        });
    }

    @Test
    void buscar_soloPorEstado() {
        assertThat(polizaRepository.buscar(null, EstadoPoliza.CANCELADA))
                .isNotEmpty()
                .allSatisfy(poliza -> assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA));
    }
}
