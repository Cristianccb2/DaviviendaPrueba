package com.prueba.polizas.service;

import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.exception.RecursoNoEncontradoException;
import com.prueba.polizas.exception.ReglaNegocioException;
import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PolizaService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PolizaRepository polizaRepository;
    private final CoreClient coreClient;

    public PolizaService(PolizaRepository polizaRepository, CoreClient coreClient) {
        this.polizaRepository = polizaRepository;
        this.coreClient = coreClient;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream()
                .map(PolizaResponse::desde)
                .toList();
    }

    /**
     * Incrementa canon y prima en el IPC indicado y marca la póliza como RENOVADA.
     * No recalcula fechas ni vigencias.
     */
    @Transactional
    public PolizaResponse renovar(Long id, BigDecimal ipcPorcentaje) {
        Poliza poliza = buscarPoliza(id);
        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("POLIZA_CANCELADA", "No se puede renovar una póliza cancelada");
        }

        BigDecimal factor = BigDecimal.ONE.add(ipcPorcentaje.divide(CIEN));
        BigDecimal nuevoCanon = poliza.getCanonMensual().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevaPrima = poliza.getPrima().multiply(factor).setScale(2, RoundingMode.HALF_UP);

        // Solo hay cambio efectivo si varía el estado o algún importe (p. ej. IPC 0 sobre una RENOVADA no lo es)
        boolean huboCambio = poliza.getEstado() != EstadoPoliza.RENOVADA
                || nuevoCanon.compareTo(poliza.getCanonMensual()) != 0
                || nuevaPrima.compareTo(poliza.getPrima()) != 0;

        if (huboCambio) {
            poliza.setCanonMensual(nuevoCanon);
            poliza.setPrima(nuevaPrima);
            poliza.setEstado(EstadoPoliza.RENOVADA);
            polizaRepository.save(poliza);
            coreClient.notificarActualizacion(poliza.getId());
        }
        return PolizaResponse.desde(poliza);
    }

    /** Cancela la póliza y todos sus riesgos en una sola transacción. Es idempotente. */
    @Transactional
    public PolizaResponse cancelar(Long id) {
        Poliza poliza = buscarPoliza(id);
        if (poliza.getEstado() != EstadoPoliza.CANCELADA) {
            poliza.setEstado(EstadoPoliza.CANCELADA);
            // Los riesgos son entidades gestionadas: JPA persiste el cambio al confirmar la transacción
            poliza.getRiesgos().forEach(riesgo -> riesgo.setEstado(EstadoRiesgo.CANCELADO));
            polizaRepository.save(poliza);
            coreClient.notificarActualizacion(poliza.getId());
        }
        return PolizaResponse.desde(poliza);
    }

    private Poliza buscarPoliza(Long id) {
        return polizaRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.poliza(id));
    }
}
