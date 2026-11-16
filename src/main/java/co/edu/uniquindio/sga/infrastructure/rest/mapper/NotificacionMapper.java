package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.NotificacionResponse;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/** Dominio → respuesta de la bandeja de notificaciones: los objetos de valor viajan como texto. */
@Component
public class NotificacionMapper {

    public NotificacionResponse aResponse(RegistroNotificacion registro) {
        return new NotificacionResponse(
                registro.tipo(),
                registro.destinatario() == null ? null : registro.destinatario().valor(),
                registro.asunto(),
                registro.mensaje(),
                registro.codigoReserva() == null ? null : registro.codigoReserva().valor(),
                registro.estado(),
                registro.detalle(),
                registro.registradaEn());
    }
}
