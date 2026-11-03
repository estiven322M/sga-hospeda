package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Movimiento del folio con signo (7.7, RN-16): corrige un cargo sin editarlo ni borrarlo.
 * Positivo suma a lo que debe el huésped; negativo es el movimiento inverso (por ejemplo, el reverso
 * del alojamiento al cancelar o la diferencia a favor al modificar una reserva).
 */
public record Ajuste(String concepto, Dinero valor) {

    public Ajuste {
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaDominioException("El ajuste debe tener un concepto");
        }
        if (valor == null) {
            throw new ReglaDominioException("El ajuste debe tener un valor");
        }
    }
}
