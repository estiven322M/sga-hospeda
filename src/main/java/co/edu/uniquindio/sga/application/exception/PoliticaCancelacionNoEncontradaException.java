package co.edu.uniquindio.sga.application.exception;

public class PoliticaCancelacionNoEncontradaException extends RecursoNoEncontradoException {

    /** @param criterio descripción de lo buscado, por ejemplo "versión 2" o "vigente el 2026-11-02" */
    public PoliticaCancelacionNoEncontradaException(String criterio) {
        super("No se encontró la política de cancelación (" + criterio + ")");
    }
}
