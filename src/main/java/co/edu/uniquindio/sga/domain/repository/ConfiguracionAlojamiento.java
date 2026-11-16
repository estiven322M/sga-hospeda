package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

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

    /**
     * RP-01 (L-20): mínimo de noches por nombre de temporada (p. ej. ALTA=3). Las temporadas que no figuran no exigen
     * mínimo. Vacío significa que ninguna temporada exige estancia mínima.
     */
    Map<String, Integer> estanciaMinimaPorTemporada();

    /** RP-02 (L-20): desde cuántas noches se aplica el descuento por estancia larga. */
    int nochesDescuentoEstanciaLarga();

    /** RP-02 (L-20): porcentaje del total que se descuenta por estancia larga (0 = sin descuento). */
    int porcentajeDescuentoEstanciaLarga();

    /** RP-03 (L-20): edad en años desde la que un ocupante cuenta como adulto responsable. */
    int edadAdultoResponsableAnios();

}
