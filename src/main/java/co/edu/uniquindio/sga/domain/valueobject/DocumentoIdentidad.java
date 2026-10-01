package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Documento de identidad de una persona del negocio (titular u ocupante).
 */

public record DocumentoIdentidad(String numero) {
	
	
	
	public DocumentoIdentidad{
		if(numero == null || numero.isBlank()) {
			throw new ReglaDominioException("El documento de identidad es obligatorio");
		}
		numero=numero.trim();
		
	}

}
