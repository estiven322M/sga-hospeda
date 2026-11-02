package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** CU-08. El valor va en pesos COP, sin decimales. Un mismo folio admite pagos con medios distintos. */
public record RegistrarPagoRequest(
        @NotBlank(message = "El medio de pago es obligatorio")
        String medio,
        @NotNull(message = "La fecha del pago es obligatoria")
        LocalDate fecha,
        @NotNull(message = "El valor del pago es obligatorio")
        @Positive(message = "El valor del pago debe ser mayor que cero")
        Long valor) {
}
