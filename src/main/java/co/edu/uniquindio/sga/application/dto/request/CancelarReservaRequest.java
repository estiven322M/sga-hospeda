package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-05. Toda cancelación registra su motivo y quién la ejecuta. */
public record CancelarReservaRequest(
        @NotBlank(message = "El motivo de la cancelación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo,
        @NotBlank(message = "Debe indicarse quién cancela la reserva")
        String autor) {
}
