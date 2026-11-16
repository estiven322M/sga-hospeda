package co.edu.uniquindio.sga.application.exception;

/** El alojamiento aún no se ha sembrado ni registrado. */
public class AlojamientoNoEncontradoException extends RecursoNoEncontradoException {

    public AlojamientoNoEncontradoException() {
        super("Aún no se han registrado los datos del alojamiento");
    }
}
