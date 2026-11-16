package co.edu.uniquindio.sga.application.exception;

/** No existe el usuario pedido. Responde 404. */
public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

    public UsuarioNoEncontradoException(String descripcion) {
        super("No existe un usuario " + descripcion);
    }
}
