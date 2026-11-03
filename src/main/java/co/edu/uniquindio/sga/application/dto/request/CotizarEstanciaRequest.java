package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** CU-01. La composición del grupo (con fechas de nacimiento) define cuántos ocupantes son facturables. */
public record CotizarEstanciaRequest(
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacionApartamento,
        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,
        @NotEmpty(message = "La cotización requiere la composición del grupo")
        List<@Valid OcupanteRequest> ocupantes) {
}
