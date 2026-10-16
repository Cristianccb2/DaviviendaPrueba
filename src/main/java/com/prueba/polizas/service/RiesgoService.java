package com.prueba.polizas.service;

import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.exception.RecursoNoEncontradoException;
import com.prueba.polizas.exception.ReglaNegocioException;
import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import com.prueba.polizas.repository.RiesgoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final PolizaRepository polizaRepository;
    private final CoreClient coreClient;

    public RiesgoService(RiesgoRepository riesgoRepository, PolizaRepository polizaRepository, CoreClient coreClient) {
        this.riesgoRepository = riesgoRepository;
        this.polizaRepository = polizaRepository;
        this.coreClient = coreClient;
    }

    /** Devuelve todos los riesgos de la póliza, incluidos los cancelados. */
    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarPorPoliza(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw RecursoNoEncontradoException.poliza(polizaId);
        }
        return riesgoRepository.findByPolizaIdOrderByIdAsc(polizaId).stream()
                .map(RiesgoResponse::desde)
                .toList();
    }

    /** Solo las pólizas COLECTIVA no canceladas admiten nuevos riesgos. */
    @Transactional
    public RiesgoResponse agregar(Long polizaId, String descripcion) {
        Poliza poliza = polizaRepository.findById(polizaId)
                .orElseThrow(() -> RecursoNoEncontradoException.poliza(polizaId));
        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException("TIPO_POLIZA_NO_PERMITE_RIESGOS",
                    "Solo las pólizas colectivas permiten agregar riesgos");
        }
        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("POLIZA_CANCELADA", "No se pueden agregar riesgos a una póliza cancelada");
        }

        Riesgo riesgo = riesgoRepository.save(new Riesgo(poliza, descripcion, EstadoRiesgo.ACTIVO));
        coreClient.notificarActualizacion(polizaId);
        return RiesgoResponse.desde(riesgo);
    }

    /** Cancela solo el riesgo indicado; la póliza y sus demás riesgos no cambian. Es idempotente. */
    @Transactional
    public RiesgoResponse cancelar(Long id) {
        Riesgo riesgo = riesgoRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.riesgo(id));
        if (riesgo.getEstado() == EstadoRiesgo.ACTIVO) {
            riesgo.setEstado(EstadoRiesgo.CANCELADO);
            riesgoRepository.save(riesgo);
            coreClient.notificarActualizacion(riesgo.getPoliza().getId());
        }
        return RiesgoResponse.desde(riesgo);
    }
}
