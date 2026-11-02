package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

/** CU-04. Cambia las fechas y/o los ocupantes; el apartamento de la reserva no cambia. */
public record ModificarReservaRequest(
        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,
        @NotEmpty(message = "La reserva debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes,
        @NotBlank(message = "Debe indicarse quién modifica la reserva")
        String autor) {
}
