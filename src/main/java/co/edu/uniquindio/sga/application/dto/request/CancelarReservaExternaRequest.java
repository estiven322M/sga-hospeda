package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Size;

/** 10.3.1: el motivo con el que el canal cancela una reserva suya. Opcional. */
public record CancelarReservaExternaRequest(
        @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
        String motivo) {
}
