package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Crea un apartamento (nace inactivo hasta tener tarifas completas). La identificación es única y estable. */
public record CrearApartamentoRequest(
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacion,
        @NotBlank(message = "El nombre del apartamento es obligatorio")
        String nombre,
        @NotNull(message = "El número de dormitorios es obligatorio")
        @Min(value = 1, message = "El apartamento debe tener al menos un dormitorio")
        Integer dormitorios,
        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos de 1 persona")
        Integer capacidad) {
}
