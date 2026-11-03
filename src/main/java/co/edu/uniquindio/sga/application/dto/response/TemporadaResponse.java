package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TemporadaResponse(String nombre, boolean base, List<PeriodoResponse> periodos) {

    public record PeriodoResponse(LocalDate desde, LocalDate hasta) {
    }
}
