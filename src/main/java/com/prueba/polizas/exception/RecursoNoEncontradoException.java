package com.prueba.polizas.exception;

/** Recurso inexistente: se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    private final String codigo;

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public static RecursoNoEncontradoException poliza(Long id) {
        return new RecursoNoEncontradoException("POLIZA_NO_ENCONTRADA", "No existe la póliza con id " + id);
    }

    public static RecursoNoEncontradoException riesgo(Long id) {
        return new RecursoNoEncontradoException("RIESGO_NO_ENCONTRADO", "No existe el riesgo con id " + id);
    }

    public String getCodigo() {
        return codigo;
    }
}
