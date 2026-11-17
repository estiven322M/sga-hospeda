package co.edu.uniquindio.sga.domain.valueobject;

import java.util.List;

/** Reporte de reservas por canal de origen de un periodo (7.9, L-19), con una fila por cada canal. */
public record ReporteReservasPorCanal(Periodo periodo, List<ReservasDeCanal> canales, int total) {

    public ReporteReservasPorCanal {
        canales = List.copyOf(canales);
    }

    /** Canceladas de todos los canales. */
    public int canceladas() {
        return canales.stream().mapToInt(ReservasDeCanal::canceladas).sum();
    }

    /** No-show de todos los canales. */
    public int noShow() {
        return canales.stream().mapToInt(ReservasDeCanal::noShow).sum();
    }
}
