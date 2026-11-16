package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoNotificacion;

/**
 * Un renglón de la bandeja de notificaciones. {@code destinatario} es nulo cuando no se conocía ningún correo
 * (estado SIN_CORREO); {@code codigoReserva} es nulo en el aviso de recuperación; {@code detalle} explica el fallo.
 */
public record NotificacionResponse(
        TipoNotificacion tipo,
        String destinatario,
        String asunto,
        String mensaje,
        String codigoReserva,
        EstadoNotificacion estado,
        String detalle,
        LocalDateTime registradaEn) {
}
