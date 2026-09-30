package co.edu.uniquindio.sga.ValueObject;

import java.time.LocalDate;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record CargoNoche(LocalDate fecha, Dinero subtotal) {
	
	public CargoNoche {
        if (fecha == null || subtotal == null) {
            throw new ReglaDominioException("El cargo por noche requiere fecha y valor");
        }
    }

}
