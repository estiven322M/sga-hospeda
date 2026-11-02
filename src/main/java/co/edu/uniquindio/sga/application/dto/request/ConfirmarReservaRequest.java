package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;

/**
 * Confirmación de la reserva. La hora estimada de llegada es opcional aquí si ya se informó al crear;
 * sin ella la reserva no puede confirmarse (RN-09).
 */
public record ConfirmarReservaRequest(
        LocalTime horaEstimadaLlegada,
        @NotBlank(message = "Debe indicarse quién confirma la reserva")
        String autor) {
}
