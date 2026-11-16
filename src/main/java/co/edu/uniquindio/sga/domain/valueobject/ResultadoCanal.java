package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Cómo terminó un mensaje del canal (bitácora, 7.8): ACEPTADA (se procesó), RECHAZADA (una regla del negocio lo impidió, por ejemplo
 * un conflicto RN-18), REPETIDA (el mismo mensaje ya se había recibido, RN-19: no duplica), INVALIDA (el mensaje venía mal armado),
 * NO_ENCONTRADA (la reserva que se quería cancelar no existe), NO_AUTORIZADA (canal desactivado) o ERROR (fallo del servidor).
 */
public enum ResultadoCanal {
    ACEPTADA,
    RECHAZADA,
    REPETIDA,
    INVALIDA,
    NO_ENCONTRADA,
    NO_AUTORIZADA,
    ERROR
}
