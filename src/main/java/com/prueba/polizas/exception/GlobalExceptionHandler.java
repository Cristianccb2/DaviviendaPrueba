package com.prueba.polizas.exception;

import com.prueba.polizas.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce las excepciones a respuestas JSON homogéneas { "codigo", "mensaje" }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage());
    }

    /** Cuerpo JSON que no cumple las anotaciones de Bean Validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoInvalido(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return respuesta(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA, mensaje);
    }

    /** Parámetros del método (p. ej. id no positivo) que no cumplen sus restricciones. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> manejarParametroInvalido(HandlerMethodValidationException ex) {
        String mensaje = ex.getAllErrors().get(0).getDefaultMessage();
        return respuesta(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA, mensaje);
    }

    /** Id no numérico o valor de enum inexistente en la ruta o en los filtros. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "Valor inválido para el parámetro '" + ex.getName() + "': " + ex.getValue();
        return respuesta(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA, mensaje);
    }

    /** Cuerpo ausente, JSON mal formado o tipo de dato incorrecto (p. ej. IPC no numérico). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA,
                "El cuerpo de la solicitud está ausente o tiene un formato inválido");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(Exception ex) {
        // Excepciones estándar de Spring MVC (ruta inexistente, método o media type no soportado)
        // conservan su código HTTP original en lugar de convertirse en 500.
        if (ex instanceof org.springframework.web.ErrorResponse errorWeb) {
            HttpStatusCode status = errorWeb.getStatusCode();
            return respuesta(status, HttpStatus.valueOf(status.value()).name(), errorWeb.getBody().getDetail());
        }
        log.error("Error inesperado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error inesperado");
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatusCode status, String codigo, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorResponse(codigo, mensaje));
    }
}
