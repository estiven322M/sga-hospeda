package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Tramos ordenados de mayor a menor antelación. */
public record PoliticaCancelacionResponse(int version, LocalDate vigenteDesde, List<TramoResponse> tramos) {

    public record TramoResponse(int horasAntelacionMinima, int porcentajeRetencion) {
    }
}
