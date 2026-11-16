package co.edu.uniquindio.sga.domain.valueobject;

/** Resultado de un intento de aviso: se envió, falló el proveedor o no había a quién escribirle. */
public enum EstadoNotificacion {
    ENVIADA,
    FALLIDA,
    SIN_CORREO
}
