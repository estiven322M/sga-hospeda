package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** CU-02. Los precios se expresan en pesos COP, sin decimales, y filtran por el total de la estancia. */
public record ConsultarDisponibilidadRequest(
        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,
        @NotEmpty(message = "La consulta requiere la composición del grupo")
        List<@Valid OcupanteRequest> ocupantes,
        @Positive(message = "La capacidad mínima debe ser positiva")
        Integer capacidadMinima,
        @PositiveOrZero(message = "El precio mínimo no puede ser negativo")
        Long precioMinimo,
        @PositiveOrZero(message = "El precio máximo no puede ser negativo")
        Long precioMaximo) {
}
