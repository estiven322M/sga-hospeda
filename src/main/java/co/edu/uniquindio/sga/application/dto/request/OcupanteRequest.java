package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

/** Un ocupante del grupo. Bean Validation revisa la forma; la edad y la facturación las decide el dominio. */
public record OcupanteRequest(
        @NotBlank(message = "El documento del ocupante es obligatorio")
        String documento,
        @NotBlank(message = "El nombre del ocupante es obligatorio")
        String nombre,
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento) {
}
