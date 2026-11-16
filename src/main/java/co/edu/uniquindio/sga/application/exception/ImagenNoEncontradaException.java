package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** El apartamento no tiene una imagen con ese identificador en su galería (o ya fue retirada). Responde 404. */
public class ImagenNoEncontradaException extends RecursoNoEncontradoException {

    public ImagenNoEncontradaException(IdentificacionApartamento apartamento, String id) {
        super("El apartamento " + apartamento.valor() + " no tiene una imagen con el identificador " + id);
    }
}
