package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

public record BloqueoResponse(
        String identificacionApartamento,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String motivo) {
}
