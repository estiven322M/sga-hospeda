package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Valor por ocupante facturable y por noche, en pesos COP sin decimales. La temporada va en la URL. */
public record RegistrarTarifaRequest(
        @NotNull(message = "El valor de la tarifa es obligatorio")
        @Positive(message = "El valor de la tarifa debe ser mayor que cero")
        Long valorPorOcupanteNoche) {
}
