package co.edu.uniquindio.sga.domain.valueobject;

import java.time.Duration;
import java.time.LocalTime;

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
     * Definición 3.3 / RN-20: la entrada el mismo día solo es posible si el tiempo de preparación
     * cabe en la ventana entre la hora de salida y la hora de entrada del alojamiento.
     * Ejemplo: salida 11:00 y entrada 15:00 (ventana de 4 h): con 3 h se permite, con 5 h no.
     */
    public boolean permiteEntradaElMismoDia(LocalTime horaSalida, LocalTime horaEntrada) {
        if (horaSalida == null || horaEntrada == null) {
            throw new ReglaDominioException("Las horas de salida y entrada son obligatorias");
        }
        long ventanaMinutos = Math.max(0, Duration.between(horaSalida, horaEntrada).toMinutes());
        return ventanaMinutos >= horas * 60L;
    }

}
