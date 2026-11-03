package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de las operaciones de la reserva que solo necesitan registrar quién las ejecuta (llegada, salida, no-show). */
public record AutorRequest(
        @NotBlank(message = "Debe indicarse quién ejecuta la operación")
        String autor) {
}
