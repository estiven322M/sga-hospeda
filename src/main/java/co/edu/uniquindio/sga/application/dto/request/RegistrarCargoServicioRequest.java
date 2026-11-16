package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** L-17, 7.7: cargar un servicio adicional al folio. El valor del cargo lo calcula el dominio (valor del servicio × cantidad). */
public record RegistrarCargoServicioRequest(
        @NotBlank(message = "El código del servicio adicional es obligatorio")
        String codigoServicio,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100, message = "La cantidad no puede superar 100")
        Integer cantidad) {
}
