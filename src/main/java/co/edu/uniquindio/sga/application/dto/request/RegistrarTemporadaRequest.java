package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Registra una temporada. La temporada base no lleva periodos (cubre las fechas que ninguna otra reclama);
 * las demás deben llevar al menos uno. Los periodos incluyen ambos extremos.
 */
public record RegistrarTemporadaRequest(
        @NotBlank(message = "El nombre de la temporada es obligatorio")
        String nombre,
        @NotNull(message = "Debe indicarse si es la temporada base")
        Boolean base,
        List<@Valid PeriodoRequest> periodos) {

    public record PeriodoRequest(
            @NotNull(message = "La fecha de inicio del periodo es obligatoria")
            LocalDate desde,
            @NotNull(message = "La fecha de fin del periodo es obligatoria")
            LocalDate hasta) {
    }
}
