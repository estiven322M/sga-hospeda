package co.edu.uniquindio.sga.domain.exception;

/**
 * RN-19: el mismo mensaje (canal + identificador externo) llegó dos veces. Sigue siendo una regla del negocio (409, como hoy), pero se
 * distingue de un conflicto de disponibilidad (RN-18) para que la bitácora del canal diga REPETIDA y no RECHAZADA.
 */
public class MensajeRepetidoException extends ReglaDominioException {

    public MensajeRepetidoException(String mensaje) {
        super(mensaje);
    }
}
