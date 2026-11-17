package co.edu.uniquindio.sga.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reporte de ocupación (7.9): porcentaje = noches vendidas / (apartamentos activos × noches del periodo), con dos decimales. */
public record ReporteOcupacionResponse(LocalDate desde, LocalDate hasta, int apartamentosActivos, int nochesPeriodo,
                                       int nochesDisponibles, int nochesVendidas, BigDecimal porcentajeOcupacion) {
}
