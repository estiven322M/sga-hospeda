package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.exception.NotificacionFallidaException;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;

/**
 * Puerto de salida de las notificaciones (10.3.2-A, I-01): el dominio pide avisar, no sabe por dónde.
 * Adaptadores: NotificadorLocal (por defecto, sin internet) y NotificadorCorreoSmtp (Gmail, con tiempo máximo).
 */
public interface Notificador {

    /**
     * Envía el aviso al destinatario de {@code notificacion} (que no es nulo: sin correo no se llama al puerto).
     *
     * @throws NotificacionFallidaException si el proveedor no pudo enviarlo; quien llama la captura (I-05)
     */
    void enviar(Notificacion notificacion);
}
