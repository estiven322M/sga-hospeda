package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record Cargo(String concepto, Dinero valor) {
	
	public Cargo{
		if(concepto == null || concepto.isBlank()) {
			throw new ReglaDominioException("El cargo debe tener un concepto");
		}
		if (valor == null || valor.esNegativo()) {
			throw new ReglaDominioException("El valor del cargo no puede ser negativo");
		}
	}

}
