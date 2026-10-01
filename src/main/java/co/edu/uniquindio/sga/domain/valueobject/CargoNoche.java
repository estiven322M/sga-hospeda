package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDate;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;


/**
 * Una línea del desglose noche por noche (RN-05, 7.4).
 * Inmutable: una noche liquidada no se recalcula, se reemplaza el desglose completo.
 */

public record CargoNoche(
		LocalDate noche,
		String temporada,
		Dinero tarifaPorOcupante,
		int ocupantesFacturables) {
	
	public CargoNoche {
        if (noche == null) {
            throw new ReglaDominioException("El cargo de la noche debe indicar la fecha");
        }
        if (temporada == null || temporada.isBlank()) {
            throw new ReglaDominioException("Toda noche liquidada pertenece a una temporada");
        }
        if (tarifaPorOcupante == null || tarifaPorOcupante.esNegativo()) {
            throw new ReglaDominioException("La tarifa de la noche no puede ser negativa");
        }
        if (ocupantesFacturables < 0) {
            throw new ReglaDominioException("Los ocupantes facturables no pueden ser negativos");
        }
    }
	
	/** RN-05: tarifa de la noche por el número de ocupantes facturables. */
    public Dinero subtotal() {
        return tarifaPorOcupante.por(ocupantesFacturables);
    }

}
