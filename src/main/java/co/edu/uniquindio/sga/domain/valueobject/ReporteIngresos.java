package co.edu.uniquindio.sga.domain.valueobject;

import java.util.List;

/**
 * Reporte de ingresos de un periodo (7.9, L-19): la suma de los PAGOS registrados en el periodo, discriminada por
 * medio de pago. Los cargos que no se han pagado no son ingreso.
 */
public record ReporteIngresos(Periodo periodo, List<IngresoPorMedio> porMedio, Dinero total) {

    public ReporteIngresos {
        porMedio = List.copyOf(porMedio);
    }
}
