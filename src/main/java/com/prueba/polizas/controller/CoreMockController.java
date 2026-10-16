package com.prueba.polizas.controller;

import com.prueba.polizas.dto.CoreEventoRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** CORE simulado: solo registra el evento en el log; no modifica BD ni reenvía nada. */
@RestController
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/core-mock/evento")
    public Map<String, String> registrarEvento(@Valid @RequestBody CoreEventoRequest request) {
        log.info("CORE_MOCK intento evento={} polizaId={}", request.evento(), request.polizaId());
        return Map.of("mensaje", "Evento registrado");
    }
}
