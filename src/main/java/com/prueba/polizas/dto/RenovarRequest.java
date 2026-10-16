package com.prueba.polizas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** IPC en porcentaje: 5.20 significa 5,20 %. */
public record RenovarRequest(
        @NotNull(message = "ipcPorcentaje es obligatorio")
        @DecimalMin(value = "0", message = "ipcPorcentaje debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "ipcPorcentaje debe estar entre 0 y 100")
        BigDecimal ipcPorcentaje) {
}
