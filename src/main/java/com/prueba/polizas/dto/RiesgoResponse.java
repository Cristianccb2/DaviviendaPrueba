package com.prueba.polizas.dto;

import com.prueba.polizas.model.EstadoRiesgo;
import com.prueba.polizas.model.Riesgo;

public record RiesgoResponse(Long id, Long polizaId, String descripcion, EstadoRiesgo estado) {

    public static RiesgoResponse desde(Riesgo riesgo) {
        return new RiesgoResponse(riesgo.getId(), riesgo.getPoliza().getId(), riesgo.getDescripcion(), riesgo.getEstado());
    }
}
