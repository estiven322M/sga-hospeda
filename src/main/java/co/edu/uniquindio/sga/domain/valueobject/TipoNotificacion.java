package co.edu.uniquindio.sga.domain.valueobject;

/** Qué se le avisa al titular (10.3.2, opción A). El código de recuperación es el único aviso sensible. */
public enum TipoNotificacion {
    RESERVA_CREADA,
    RESERVA_CONFIRMADA,
    RESERVA_CANCELADA,
    CODIGO_RECUPERACION;

    /** Un aviso sensible lleva un secreto en su mensaje: los adaptadores reales no lo dejan en ningún registro. */
    public boolean esSensible() {
        return this == CODIGO_RECUPERACION;
    }
}
