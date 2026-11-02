package co.edu.uniquindio.sga.application.exception;

/**
 * Base de las excepciones de aplicación "el identificador no existe". El manejador global
 * la traduce a HTTP 404 con una sola regla, sin importar el recurso.
 */
public abstract class RecursoNoEncontradoException extends RuntimeException {

    protected RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
