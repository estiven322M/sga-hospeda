package co.edu.uniquindio.sga.application.exception;

/**
 * Una operación sobre usuarios que el sistema no permite (contraseña actual incorrecta, desactivarse a sí mismo...).
 * Responde 409. Como el usuario es un concepto de acceso y no del dominio (3.6), vive en application; las reglas que
 * dependen de las reservas (no desactivar a un titular con reservas activas) sí son del dominio.
 */
public class OperacionUsuarioNoPermitidaException extends RuntimeException {

    public OperacionUsuarioNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
