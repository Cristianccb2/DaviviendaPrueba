package com.prueba.polizas.dto;

import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;

import java.math.BigDecimal;

public record PolizaResponse(Long id, TipoPoliza tipo, EstadoPoliza estado, BigDecimal canonMensual, BigDecimal prima) {

    public static PolizaResponse desde(Poliza poliza) {
        return new PolizaResponse(poliza.getId(), poliza.getTipo(), poliza.getEstado(),
                poliza.getCanonMensual(), poliza.getPrima());
    }
}
