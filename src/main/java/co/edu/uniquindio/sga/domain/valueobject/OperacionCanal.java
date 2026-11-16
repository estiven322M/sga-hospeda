package co.edu.uniquindio.sga.domain.valueobject;

/** Las operaciones que el SGA expone a un canal externo: las tres del contrato (10.3.1) y la consulta de una reserva propia. */
public enum OperacionCanal {
    CONSULTAR_DISPONIBILIDAD,
    CREAR_RESERVA,
    CANCELAR_RESERVA,
    CONSULTAR_RESERVA
}
