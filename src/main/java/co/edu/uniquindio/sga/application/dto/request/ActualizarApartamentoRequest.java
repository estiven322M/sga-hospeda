package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Edita los datos de un apartamento. La identificación no se puede cambiar. */
public record ActualizarApartamentoRequest(
        @NotBlank(message = "El nombre del apartamento es obligatorio")
        String nombre,
        @NotNull(message = "El número de dormitorios es obligatorio")
        @Min(value = 1, message = "El apartamento debe tener al menos un dormitorio")
        Integer dormitorios,
        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos de 1 persona")
        Integer capacidad) {
}
