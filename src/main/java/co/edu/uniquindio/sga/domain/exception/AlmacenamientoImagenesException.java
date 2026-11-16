package co.edu.uniquindio.sga.domain.exception;

/**
 * El almacén externo de imágenes no pudo guardar una (tiempo agotado, credenciales rechazadas, sin conexión...). NO es una regla
 * de negocio ni un error del cliente: el administrador recibe un aviso claro (503) y puede reintentar. No afecta a nada más:
 * las imágenes ya guardadas se siguen mostrando y reservar, registrar, cobrar y cerrar un folio no dependen del almacén (I-05).
 */
public class AlmacenamientoImagenesException extends RuntimeException {

    public AlmacenamientoImagenesException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public AlmacenamientoImagenesException(String mensaje) {
        super(mensaje);
    }
}
