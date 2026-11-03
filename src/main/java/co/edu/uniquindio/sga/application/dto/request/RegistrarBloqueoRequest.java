package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** CU-09. Bloquea las noches [fechaInicio, fechaFin): la fecha fin no se bloquea, igual que en una estancia. */
public record RegistrarBloqueoRequest(
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,
        @NotBlank(message = "Todo bloqueo debe justificar un motivo")
        String motivo) {
}
