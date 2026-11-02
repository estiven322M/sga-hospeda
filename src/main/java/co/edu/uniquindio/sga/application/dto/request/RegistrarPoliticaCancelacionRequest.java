package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Nueva versión de la política de cancelación. La versión la asigna el sistema. Cada tramo indica la antelación
 * mínima (en horas) a la entrada y el porcentaje que se retiene; debe haber uno de antelación 0.
 */
public record RegistrarPoliticaCancelacionRequest(
        @NotNull(message = "La fecha desde la que rige la política es obligatoria")
        LocalDate vigenteDesde,
        @NotEmpty(message = "La política requiere tramos de retención")
        List<@Valid TramoRequest> tramos) {

    public record TramoRequest(
            @NotNull(message = "La antelación mínima del tramo es obligatoria")
            @Min(value = 0, message = "La antelación no puede ser negativa")
            Integer horasAntelacionMinima,
            @NotNull(message = "El porcentaje de retención es obligatorio")
            @Min(value = 0, message = "El porcentaje no puede ser negativo")
            @Max(value = 100, message = "El porcentaje no puede superar 100")
            Integer porcentajeRetencion) {
    }
}
