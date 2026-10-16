package com.prueba.polizas.controller;

import com.prueba.polizas.dto.CrearRiesgoRequest;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.service.RiesgoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RiesgoController {

    private final RiesgoService riesgoService;

    public RiesgoController(RiesgoService riesgoService) {
        this.riesgoService = riesgoService;
    }

    @GetMapping("/polizas/{id}/riesgos")
    public List<RiesgoResponse> listarPorPoliza(@PathVariable @Positive(message = "El id debe ser un entero positivo") Long id) {
        return riesgoService.listarPorPoliza(id);
    }

    @PostMapping("/polizas/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    public RiesgoResponse agregar(@PathVariable @Positive(message = "El id debe ser un entero positivo") Long id,
                                  @Valid @RequestBody CrearRiesgoRequest request) {
        return riesgoService.agregar(id, request.descripcion());
    }

    @PostMapping("/riesgos/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable @Positive(message = "El id debe ser un entero positivo") Long id) {
        return riesgoService.cancelar(id);
    }
}
