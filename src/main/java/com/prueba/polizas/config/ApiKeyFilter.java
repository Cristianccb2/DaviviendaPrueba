package com.prueba.polizas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prueba.polizas.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Exige el header x-api-key en todas las rutas (incluido el CORE mock).
 * Si falta o es incorrecto responde 401 sin llegar al controlador. El valor nunca se registra en logs.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "x-api-key";

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${seguridad.api-key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (apiKey.equals(request.getHeader(HEADER_API_KEY))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse("API_KEY_INVALIDA", "API key faltante o incorrecta"));
    }
}
