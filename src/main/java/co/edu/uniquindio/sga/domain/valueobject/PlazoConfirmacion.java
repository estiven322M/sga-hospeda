package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDateTime;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record PlazoConfirmacion(int horas) {
	
	public PlazoConfirmacion {
        if (horas < 1) {
            throw new ReglaDominioException("El plazo de confirmación debe ser de al menos una hora");
        }
    }

    public boolean venceAntesDe(LocalDateTime creacion, LocalDateTime momento) {
        return momento.isAfter(creacion.plusHours(horas));
    }

    /**
     * Instante límite: una reserva creada estrictamente antes de él ya superó el plazo en {@code ahora}.
     * Es equivalente a {@link #venceAntesDe} y permite que un puerto de consulta filtre sin conocer el plazo.
     */
    public LocalDateTime limiteDeCreacion(LocalDateTime ahora) {
        return ahora.minusHours(horas);
    }

}
