package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

/**
 * Un mensaje intercambiado con un canal externo (bitácora, 7.8): cuándo, qué canal, qué operación, con qué identificador externo,
 * cómo terminó y con qué código HTTP. Es una constancia inmutable: se agrega y se consulta, nunca se edita ni se borra. El detalle
 * es una explicación breve (el motivo de un rechazo); nunca lleva la credencial.
 */
public record EventoCanal(
        LocalDateTime registradoEn,
        String canal,
        OperacionCanal operacion,
        String identificadorExterno,
        ResultadoCanal resultado,
        int codigoHttp,
        String detalle) {

    public static final int MAXIMO_DETALLE = 300;

    public EventoCanal {
        if (registradoEn == null) {
            throw new ReglaDominioException("El evento del canal debe tener fecha y hora");
        }
        if (canal == null || canal.isBlank()) {
            throw new ReglaDominioException("El evento debe indicar el canal");
        }
        if (operacion == null || resultado == null) {
            throw new ReglaDominioException("El evento debe indicar la operación y su resultado");
        }
        if (codigoHttp < 100 || codigoHttp > 599) {
            throw new ReglaDominioException("El código HTTP del evento no es válido: " + codigoHttp);
        }
        if (detalle != null && detalle.length() > MAXIMO_DETALLE) {
            detalle = detalle.substring(0, MAXIMO_DETALLE - 1) + "…";
        }
    }
}
