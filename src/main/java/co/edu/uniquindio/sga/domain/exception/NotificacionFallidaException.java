package co.edu.uniquindio.sga.domain.exception;

/**
 * El proveedor de correo no pudo enviar el aviso (tiempo agotado, credenciales rechazadas, sin conexión...).
 * NO es una regla de negocio: nunca llega al cliente como 409. Quien notifica la captura, la registra en el log y
 * sigue: el fallo de una integración no puede impedir reservar, registrar, cobrar ni cerrar un folio (I-05).
 */
public class NotificacionFallidaException extends RuntimeException {

    public NotificacionFallidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public NotificacionFallidaException(String mensaje) {
        super(mensaje);
    }
}
