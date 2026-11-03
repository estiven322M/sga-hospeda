package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

public class ReservaNoEncontradaException extends RecursoNoEncontradoException {

    public ReservaNoEncontradaException(CodigoReserva codigo) {
        super("No se encontró la reserva con código: " + codigo.valor());
    }
}
