package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

public class FolioNoEncontradoException extends RecursoNoEncontradoException {

    public FolioNoEncontradoException(CodigoReserva reserva) {
        super("No se encontró el folio de la reserva: " + reserva.valor());
    }
}
