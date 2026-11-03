package co.edu.uniquindio.sga.domain.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/**
 * Política de cancelación del alojamiento (F-07, 7.4). Está versionada: la reserva congela
 * la versión vigente al crearse y calcula siempre con ella (RN-13, RN-22). Se identifica por su versión.
 */
public class PoliticaCancelacion {

    private final VersionPolitica version;
    private final LocalDate vigenteDesde;
    private final List<TramoRetencion> tramos; // ordenados de mayor a menor antelación

    public PoliticaCancelacion(VersionPolitica version, LocalDate vigenteDesde, List<TramoRetencion> tramos) {
        if (version == null) throw new ReglaDominioException("La política debe tener una versión");
        if (vigenteDesde == null) throw new ReglaDominioException("La política debe indicar desde cuándo rige");
        if (tramos == null || tramos.size() < 2) {
            throw new ReglaDominioException("La política de cancelación requiere al menos dos tramos");
        }
        if (tramos.stream().map(TramoRetencion::horasAntelacionMinima).distinct().count() != tramos.size()) {
            throw new ReglaDominioException("Los tramos de la política no pueden repetir la antelación");
        }
        List<TramoRetencion> ordenados = tramos.stream()
                .sorted(Comparator.comparingInt(TramoRetencion::horasAntelacionMinima).reversed())
                .toList();
        // El último tramo debe cubrir cualquier antelación (incluida la cancelación tardía)
        if (ordenados.get(ordenados.size() - 1).horasAntelacionMinima() != 0) {
            throw new ReglaDominioException(
                    "La política debe tener un tramo de antelación 0 que cubra las cancelaciones tardías");
        }
        this.version = version;
        this.vigenteDesde = vigenteDesde;
        this.tramos = ordenados;
    }

    /** Valor que se retiene al cancelar con la antelación dada, redondeado al peso (regla 14). */
    public Dinero retencion(Dinero total, Duration antelacion) {
        long horas = antelacion.toHours();
        TramoRetencion aplicable = tramos.stream()
                .filter(t -> horas >= t.horasAntelacionMinima())
                .findFirst()
                .orElse(tramos.get(tramos.size() - 1)); // antelación negativa: aplica el tramo más tardío
        return porcentaje(total, aplicable.porcentajeRetencion());
    }

    /** Valor que se devuelve al huésped: lo cobrado menos la retención. */
    public Dinero devolucion(Dinero total, Duration antelacion) {
        return total.menos(retencion(total, antelacion));
    }

    private static Dinero porcentaje(Dinero total, int porcentaje) {
        BigDecimal valor = total.valor().multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return new Dinero(valor);
    }

    public VersionPolitica getVersion() { return version; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public List<TramoRetencion> getTramos() { return tramos; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PoliticaCancelacion otra)) return false;
        return version.equals(otra.version);
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }
}
