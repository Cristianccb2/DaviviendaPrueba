package com.prueba.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRiesgoRequest(
        @NotBlank(message = "descripcion es obligatoria y no puede estar vacía")
        @Size(max = 255, message = "descripcion admite máximo 255 caracteres")
        String descripcion) {
}
