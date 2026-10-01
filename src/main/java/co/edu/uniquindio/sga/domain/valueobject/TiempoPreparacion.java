package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Tiempo de preparación, en horas, requerido entre la salida de un grupo 
 * y la entrada del siguiente en el mismo día (3.3, L-13).
 */

public record TiempoPreparacion(int horas) {
	
	public TiempoPreparacion {
        if (horas < 0) {
            throw new ReglaDominioException("El tiempo de preparación no puede ser negativo");
        }
    }

    /**
     * Según la sección 3.3: si el tiempo configurado excede la ventana entre la salida 
     * y la entrada, el apartamento no puede recibir una entrada el mismo día.
     * Si las horas de preparación son 0 (o menores a la ventana estándar del alojamiento), 
     * se permite el mismo día.
     */
    public boolean permiteEntradaElMismoDia() {
        // Por defecto, si el tiempo de preparación es 0, la entrada el mismo día está permitida.
        // Puedes ajustar esta lógica según las horas de salida/entrada específicas de tu Ficha de Alojamiento.
        return this.horas == 0; 
    }

}
