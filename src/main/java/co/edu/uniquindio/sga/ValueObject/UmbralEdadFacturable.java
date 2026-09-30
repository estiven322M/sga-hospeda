package co.edu.uniquindio.sga.ValueObject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Edad a partir de la cual un ocupante genera cargo (L-09).
 * El valor lo define cada alojamiento: es configuración, no una constante.
 */

public record UmbralEdadFacturable(int anios) {
	
	public UmbralEdadFacturable {
        if (anios < 0 || anios > 30) {
            throw new ReglaDominioException(
                "El umbral de edad facturable debe estar entre 0 y 30 años");
        }
    }

}
