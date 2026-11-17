package co.edu.uniquindio.sga.application.dto.request;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Novedad sobre un apartamento (7.6). El autor y la fecha NO viajan: salen del token y del reloj del servidor. */
public record RegistrarNovedadRequest(
        @NotBlank(message = "La descripción de la novedad es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,
        @NotNull(message = "La gravedad es obligatoria")
        GravedadNovedad gravedad) {
}
