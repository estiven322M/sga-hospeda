package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Identificador con el que el canal externo conoce una reserva (RN-19). Junto con el canal identifica el mensaje
 * recibido: la misma pareja nunca crea dos reservas. Se normaliza sin espacios en los extremos.
 */
public record IdentificadorExterno(String valor) {

    public static final int LONGITUD_MAXIMA = 60;

    public IdentificadorExterno {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("El identificador externo es obligatorio");
        }
        valor = valor.trim();
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new ReglaDominioException(
                    "El identificador externo no puede superar " + LONGITUD_MAXIMA + " caracteres");
        }
    }
}
