package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record VersionPolitica(int numero) {
	
	public VersionPolitica{
		if(numero<1) {
			throw new ReglaDominioException("La version de la política debe ser mayor que cero");
		}
	}
}
