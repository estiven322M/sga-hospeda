package co.edu.uniquindio.sga.application.exception;

/**
 * El registro choca con un usuario existente (mismo correo o mismo documento). Responde 409 (Guía 12, 7.5).
 * No es una regla del dominio: el usuario es un concepto de acceso (3.6), por eso vive en application.
 */
public class UsuarioYaRegistradoException extends RuntimeException {

    public UsuarioYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}
