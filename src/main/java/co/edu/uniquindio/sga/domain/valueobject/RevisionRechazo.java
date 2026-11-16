package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** La revisión que el administrador hace de un conflicto del canal externo (7.8): su nota, quién y cuándo. */
public record RevisionRechazo(String nota, String revisadoPor, LocalDateTime revisadaEn) {

    public static final int MAXIMO_NOTA = 500;

    public RevisionRechazo {
        if (nota == null || nota.isBlank()) {
            throw new ReglaDominioException("Resolver un conflicto exige una nota: qué se revisó y qué se decidió");
        }
        nota = nota.trim();
        if (nota.length() > MAXIMO_NOTA) {
            throw new ReglaDominioException("La nota no puede superar " + MAXIMO_NOTA + " caracteres");
        }
        if (revisadoPor == null || revisadoPor.isBlank()) {
            throw new ReglaDominioException("La revisión debe indicar quién la hizo");
        }
        if (revisadaEn == null) {
            throw new ReglaDominioException("La revisión debe tener fecha y hora");
        }
    }
}
