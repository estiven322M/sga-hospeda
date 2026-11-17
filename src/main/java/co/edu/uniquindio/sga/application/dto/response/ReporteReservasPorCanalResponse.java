package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

/** Reporte de reservas por canal de origen (7.9): el total de cada canal incluye canceladas y no-show. */
public record ReporteReservasPorCanalResponse(LocalDate desde, LocalDate hasta, List<ReservasDeCanalResponse> canales,
                                              int total, int canceladas, int noShow) {

    public record ReservasDeCanalResponse(CanalOrigen canal, int total, int canceladas, int noShow) {
    }
}
