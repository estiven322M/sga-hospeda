package co.edu.uniquindio.sga.application.exception;

/** El canal pidió cancelar una reserva que no creó (o que no existe): no hay una reserva suya con ese identificador. Responde 404. */
public class ReservaExternaNoEncontradaException extends RecursoNoEncontradoException {

    public ReservaExternaNoEncontradaException(String identificadorExterno) {
        super("No hay una reserva de este canal con el identificador externo " + identificadorExterno);
    }
}
