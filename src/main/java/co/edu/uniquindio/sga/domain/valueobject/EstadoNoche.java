package co.edu.uniquindio.sga.domain.valueobject;

/** Estado de una noche de un apartamento en el calendario (7.1, 7.5). */
public enum EstadoNoche {
    /** Sin reserva activa ni bloqueo: se puede vender. */
    LIBRE,
    /** Tiene una reserva activa (PENDIENTE, CONFIRMADA o EN_CURSO). */
    RESERVADA,
    /** Está bloqueada por una decisión administrativa. */
    BLOQUEADA
}
