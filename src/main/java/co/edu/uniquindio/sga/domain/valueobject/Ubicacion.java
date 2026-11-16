package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Ubicación exacta del alojamiento (7.3): latitud y longitud en grados decimales. No es dinero, así que {@code double}
 * es adecuado; lo que importa es que sean coordenadas posibles sobre la Tierra.
 */
public record Ubicacion(double latitud, double longitud) {

    public Ubicacion {
        if (Double.isNaN(latitud) || latitud < -90 || latitud > 90) {
            throw new ReglaDominioException("La latitud debe estar entre -90 y 90 grados");
        }
        if (Double.isNaN(longitud) || longitud < -180 || longitud > 180) {
            throw new ReglaDominioException("La longitud debe estar entre -180 y 180 grados");
        }
    }
}
