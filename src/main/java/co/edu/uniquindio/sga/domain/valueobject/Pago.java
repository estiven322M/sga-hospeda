package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDate;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record Pago(String medio, LocalDate fecha, Dinero valor) {
	
	public Pago{
		
		if (medio == null || medio.isBlank()) {
            throw new ReglaDominioException("El pago debe indicar el medio utilizado");
        }
        if (fecha == null) {
            throw new ReglaDominioException("El pago debe tener una fecha");
        }
        if (valor == null || valor.esNegativo()) {
            throw new ReglaDominioException("El valor del pago no puede ser negativo");
        }
		
	}

}
