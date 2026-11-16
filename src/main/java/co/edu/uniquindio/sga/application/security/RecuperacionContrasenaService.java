package co.edu.uniquindio.sga.application.security;

import co.edu.uniquindio.sga.application.dto.request.ConfirmarRecuperacionRequest;

/**
 * Puerto de la recuperación de contraseña (7.2): código de 6 dígitos enviado al correo, vigente 15 minutos y de un solo
 * uso. Ambas operaciones son públicas y no revelan si el correo existe.
 */
public interface RecuperacionContrasenaService {

    /**
     * Si el correo es de un usuario activo, emite un código y lo envía por el puerto Notificador. Nunca lanza por
     * correo desconocido ni por un fallo del envío: la respuesta es siempre la misma.
     */
    void solicitar(String correo);

    /** Cambia la contraseña con el código. Cualquier problema con el código o el correo: el mismo 409. */
    void confirmar(ConfirmarRecuperacionRequest solicitud);
}
