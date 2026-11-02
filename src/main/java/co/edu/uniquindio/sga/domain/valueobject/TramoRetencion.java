package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Un tramo de la política de cancelación (L-10): si se cancela con al menos
 * {@code horasAntelacionMinima} horas de antelación a la entrada, se retiene ese porcentaje del valor.
 */
public record TramoRetencion(int horasAntelacionMinima, int porcentajeRetencion) {

    public TramoRetencion {
        if (horasAntelacionMinima < 0) {
            throw new ReglaDominioException("La antelación de un tramo no puede ser negativa");
        }
        if (porcentajeRetencion < 0 || porcentajeRetencion > 100) {
            throw new ReglaDominioException("El porcentaje de retención debe estar entre 0 y 100");
        }
    }
}
