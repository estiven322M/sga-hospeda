package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

public class ApartamentoNoEncontradoException extends RecursoNoEncontradoException {

    public ApartamentoNoEncontradoException(IdentificacionApartamento identificacion) {
        super("No se encontró el apartamento con identificación: " + identificacion.valor());
    }
}
