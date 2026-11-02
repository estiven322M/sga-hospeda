package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** Rango de fechas inclusivo en ambos extremos, usado para definir los periodos de una temporada (7.4). */
public record Periodo(LocalDate desde, LocalDate hasta) {

    public Periodo {
        if (desde == null || hasta == null) {
            throw new ReglaDominioException("El periodo requiere fecha de inicio y de fin");
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaDominioException("El fin del periodo no puede ser anterior a su inicio");
        }
    }

    public boolean incluye(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean seSolapaCon(Periodo otro) {
        return !desde.isAfter(otro.hasta) && !otro.desde.isAfter(hasta);
    }
}
