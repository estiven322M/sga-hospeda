package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalTime;
import java.time.ZoneId;

import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

/** Parámetros de la Ficha del alojamiento. Los valores salen de application.properties (regla 10). */
public interface ConfiguracionAlojamiento {

	TiempoPreparacion tiempoPreparacion();
    PlazoConfirmacion plazoConfirmacion();
    UmbralEdadFacturable umbralEdadFacturable();

    /** Hora desde la que el grupo puede entrar (check-in). */
    LocalTime horaEntrada();

    /** Hora máxima de salida del grupo (check-out). */
    LocalTime horaSalida();

    /** Hora del día de entrada desde la cual se puede declarar no-show. */
    LocalTime horaLimiteNoShow();

    /** Zona horaria del alojamiento (America/Bogota). */
    ZoneId zonaHoraria();

    /** Porcentaje del total que se cobra como anticipo para confirmar la reserva. */
    int anticipoPorcentaje();

}
