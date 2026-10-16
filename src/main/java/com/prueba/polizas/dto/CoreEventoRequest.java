package com.prueba.polizas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CoreEventoRequest(
        @NotNull(message = "evento es obligatorio")
        @Pattern(regexp = "ACTUALIZACION", message = "evento debe ser ACTUALIZACION")
        String evento,

        @NotNull(message = "polizaId es obligatorio")
        @Positive(message = "polizaId debe ser un entero positivo")
        Long polizaId) {
}
