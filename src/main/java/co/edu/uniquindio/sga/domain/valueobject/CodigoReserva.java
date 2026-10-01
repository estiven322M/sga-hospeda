package co.edu.uniquindio.sga.domain.valueobject;

import java.util.regex.Pattern;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record CodigoReserva(String valor) {
	
	private static final Pattern FORMATO = Pattern.compile("^RES-\\d{4}-\\d{5}$");
	
	public CodigoReserva{
		if(valor==null || valor.isBlank()) {
			throw new ReglaDominioException("El código de la reserva es obligatorio");
		}
		if(!FORMATO.matcher(valor).matches()) {
			throw new ReglaDominioException("El código de la reserva debe tener el formato RES-YYYY-NNNNN");
		}
	}

}
