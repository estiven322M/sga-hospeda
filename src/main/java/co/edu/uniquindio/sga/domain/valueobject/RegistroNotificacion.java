package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Constancia de un intento de aviso (bandeja de notificaciones). {@code mensaje} es lo que el adaptador decide dejar
 * visible; {@code detalle} explica el fallo (nulo si se envió).
 */
public record RegistroNotificacion(
        TipoNotificacion tipo,
        CorreoElectronico destinatario,
        String asunto,
        String mensaje,
        CodigoReserva codigoReserva,
        EstadoNotificacion estado,
        String detalle,
        LocalDateTime registradaEn) {

    public RegistroNotificacion {
        if (tipo == null || estado == null || registradaEn == null) {
            throw new ReglaDominioException("El registro de notificación exige tipo, estado y fecha");
        }
    }

    /** Arma la constancia de {@code notificacion} con el contenido que se quiera dejar visible. */
    public static RegistroNotificacion de(Notificacion notificacion, String mensajeVisible, EstadoNotificacion estado,
                                          String detalle, LocalDateTime cuando) {
        return new RegistroNotificacion(notificacion.tipo(), notificacion.destinatario(), notificacion.asunto(),
                mensajeVisible, notificacion.codigoReserva(), estado, detalle, cuando);
    }
}
