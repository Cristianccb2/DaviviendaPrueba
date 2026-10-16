package com.prueba.polizas.service;

import com.prueba.polizas.config.ApiKeyFilter;
import com.prueba.polizas.dto.CoreEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

/**
 * Envía el evento ACTUALIZACION al CORE (mock) por HTTP.
 * Un fallo se registra como WARN y no revierte la operación ya guardada: no hay reintentos.
 */
@Component
public class CoreClient {

    private static final Logger log = LoggerFactory.getLogger(CoreClient.class);
    private static final String EVENTO_ACTUALIZACION = "ACTUALIZACION";

    private final RestClient restClient;

    public CoreClient(RestClient.Builder builder,
                      @Value("${core.mock.base-url}") String baseUrl,
                      @Value("${seguridad.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(ApiKeyFilter.HEADER_API_KEY, apiKey)
                .build();
    }

    /** Si hay una transacción activa, el envío se difiere hasta que se confirme (commit) en H2. */
    public void notificarActualizacion(Long polizaId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(polizaId);
                }
            });
        } else {
            enviar(polizaId);
        }
    }

    private void enviar(Long polizaId) {
        log.info("CORE_CLIENT intento evento={} polizaId={}", EVENTO_ACTUALIZACION, polizaId);
        try {
            restClient.post()
                    .uri("/core-mock/evento")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CoreEventoRequest(EVENTO_ACTUALIZACION, polizaId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("CORE_CLIENT resultado=OK evento={} polizaId={}", EVENTO_ACTUALIZACION, polizaId);
        } catch (RuntimeException ex) {
            log.warn("CORE_CLIENT resultado=FALLO evento={} polizaId={} causa={}",
                    EVENTO_ACTUALIZACION, polizaId, ex.getMessage());
        }
    }
}
