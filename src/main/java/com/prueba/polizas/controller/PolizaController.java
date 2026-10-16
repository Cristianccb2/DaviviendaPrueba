package com.prueba.polizas.controller;

import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.dto.RenovarRequest;
import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.service.PolizaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado);
    }

    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable @Positive(message = "El id debe ser un entero positivo") Long id,
                                  @Valid @RequestBody RenovarRequest request) {
        return polizaService.renovar(id, request.ipcPorcentaje());
    }

    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable @Positive(message = "El id debe ser un entero positivo") Long id) {
        return polizaService.cancelar(id);
    }
}
