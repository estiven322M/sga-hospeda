package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Reporte de ingresos (7.9): pagos registrados en el periodo por medio de pago. Valores en pesos COP sin decimales. */
public record ReporteIngresosResponse(LocalDate desde, LocalDate hasta, List<IngresoPorMedioResponse> porMedio,
                                      long total, String moneda) {

    public record IngresoPorMedioResponse(String medio, long total) {
    }
}
