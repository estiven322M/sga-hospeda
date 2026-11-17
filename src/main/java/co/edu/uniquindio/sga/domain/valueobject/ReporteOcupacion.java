package co.edu.uniquindio.sga.domain.valueobject;

import java.math.BigDecimal;

/**
 * Reporte de ocupación de un periodo (7.9, L-19): noches vendidas frente a las noches disponibles
 * (apartamentos activos × noches del periodo). El porcentaje llega ya redondeado a dos decimales.
 */
public record ReporteOcupacion(Periodo periodo, int apartamentosActivos, int nochesPeriodo, int nochesVendidas,
                               BigDecimal porcentaje) {

    /** Noches que se podían vender en el periodo: apartamentos activos × noches del periodo. */
    public int nochesDisponibles() {
        return apartamentosActivos * nochesPeriodo;
    }
}
