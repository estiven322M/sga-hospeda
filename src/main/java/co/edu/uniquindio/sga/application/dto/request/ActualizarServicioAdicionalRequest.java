package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** L-17. Reemplaza los datos editables; el código no cambia. */
public record ActualizarServicioAdicionalRequest(
        @NotBlank(message = "El nombre del servicio adicional es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,
        @Size(max = 300, message = "La descripción no puede superar 300 caracteres")
        String descripcion,
        @NotNull(message = "Debe indicarse si el servicio genera cargo")
        Boolean generaCargo,
        @NotNull(message = "El valor es obligatorio (0 si no genera cargo)")
        @Min(value = 0, message = "El valor no puede ser negativo")
        Long valor) {
}
