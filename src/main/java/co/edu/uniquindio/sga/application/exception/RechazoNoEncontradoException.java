package co.edu.uniquindio.sga.application.exception;

/** No existe el conflicto del canal externo pedido. Responde 404. */
public class RechazoNoEncontradoException extends RecursoNoEncontradoException {

    public RechazoNoEncontradoException(long id) {
        super("No se encontró el conflicto del canal externo con id: " + id);
    }
}
