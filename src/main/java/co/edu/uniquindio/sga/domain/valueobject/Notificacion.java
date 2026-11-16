package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Un aviso por correo al titular (10.3.2, opción A). El destinatario es nulo cuando no se conoce ningún correo:
 * esa notificación no se envía, pero queda registrada como SIN_CORREO. El código de la reserva es nulo en el aviso
 * de recuperación de contraseña, que no pertenece a ninguna reserva.
 */
public record Notificacion(
        TipoNotificacion tipo,
        CorreoElectronico destinatario,
        String asunto,
        String mensaje,
        CodigoReserva codigoReserva,
        LocalDateTime generadaEn) {

    /** Lo que un registro permanente muestra en lugar del contenido de un aviso sensible. */
    public static final String CONTENIDO_OMITIDO = "(contenido sensible omitido)";

    public Notificacion {
        if (tipo == null) {
            throw new ReglaDominioException("El tipo de notificación es obligatorio");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new ReglaDominioException("El asunto de la notificación es obligatorio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new ReglaDominioException("El mensaje de la notificación es obligatorio");
        }
        if (generadaEn == null) {
            throw new ReglaDominioException("La fecha de la notificación es obligatoria");
        }
    }

    /** El mensaje tal como puede guardarse en un registro permanente: sin el secreto de los avisos sensibles. */
    public String mensajeRegistrable() {
        return tipo.esSensible() ? CONTENIDO_OMITIDO : mensaje;
    }
}
