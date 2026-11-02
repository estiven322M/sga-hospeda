package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

public record EventoReservaResponse(
        LocalDateTime momento,
        String accion,
        String autor,
        EstadoReserva estadoAnterior,
        EstadoReserva estadoNuevo,
        String observacion) {
}
