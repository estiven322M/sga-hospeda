package co.edu.uniquindio.sga.application.exception;

/** No existe el canal de venta pedido. Responde 404. */
public class CanalNoEncontradoException extends RecursoNoEncontradoException {

    public CanalNoEncontradoException(String codigo) {
        super("No se encontró el canal con código: " + codigo);
    }
}
