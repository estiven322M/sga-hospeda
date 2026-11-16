package co.edu.uniquindio.sga.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Valor de una estancia, congelado en la reserva (RN-22): el desglose noche por noche, más el porcentaje de descuento
 * por estancia larga con el que se calculó (RP-02).
 *
 * <ul>
 *   <li>{@link #bruto()}: suma de los subtotales de cada noche (RN-05).</li>
 *   <li>{@link #descuento()}: porcentaje sobre el bruto, aplicado UNA sola vez y redondeado al final (HALF_UP al peso).</li>
 *   <li>{@link #total()}: bruto menos descuento. Es lo que se cobra, lo que se anticipa y lo que se retiene al cancelar.</li>
 * </ul>
 */
public record ValorCongelado(List<CargoNoche> detalle, int porcentajeDescuento) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public ValorCongelado {
        if (detalle == null || detalle.isEmpty()) {
            throw new ReglaDominioException("El valor de la reserva debe tener desglose por noche");
        }
        if (porcentajeDescuento < 0 || porcentajeDescuento > 100) {
            throw new ReglaDominioException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        detalle = List.copyOf(detalle); // Copia inmutable
    }

    /** Valor sin descuento (0 %): conserva el comportamiento anterior a RP-02. */
    public ValorCongelado(List<CargoNoche> detalle) {
        this(detalle, 0);
    }

    /** Suma noche por noche, antes del descuento (RN-05). */
    public Dinero bruto() {
        return detalle.stream()
                .map(CargoNoche::subtotal)
                .reduce(Dinero.CERO, Dinero::mas);
    }

    /** RP-02: descuento sobre el bruto, redondeado una sola vez al final (peso más cercano, mitad hacia arriba). */
    public Dinero descuento() {
        if (porcentajeDescuento == 0) {
            return Dinero.CERO;
        }
        BigDecimal exacto = bruto().valor().multiply(BigDecimal.valueOf(porcentajeDescuento))
                .divide(CIEN, 0, RoundingMode.HALF_UP);
        return new Dinero(exacto);
    }

    /** Lo que efectivamente vale la estancia: bruto menos descuento. */
    public Dinero total() {
        return bruto().menos(descuento());
    }

    public int noches() {
        return detalle.size();
    }
}
