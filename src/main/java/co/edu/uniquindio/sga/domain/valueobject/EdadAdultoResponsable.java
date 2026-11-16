package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * RP-03 (L-20): edad desde la que un ocupante puede responder por el grupo. Toda reserva debe incluir al menos un
 * ocupante con esta edad o más a la fecha de entrada. Es distinta del umbral de edad facturable (L-09): un ocupante
 * de 12 a 17 años paga su cargo pero no cuenta como adulto responsable. El valor sale de application.properties.
 */
public record EdadAdultoResponsable(int anios) {

    public EdadAdultoResponsable {
        if (anios < 0 || anios > 30) {
            throw new ReglaDominioException("La edad del adulto responsable debe estar entre 0 y 30 años");
        }
    }
}
