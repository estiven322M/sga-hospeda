package co.edu.uniquindio.sga.domain.repository;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;

/**
 * Criterios opcionales para listar apartamentos. Sin características no se filtra por ellas; con varias, el apartamento
 * debe tener TODAS (Y lógico), igual que {@code Apartamento.tieneTodas} (L-06, 7.5). {@code estadoOperativo} nulo
 * significa "cualquier estado"; con valor, solo los apartamentos en esa condición física (3.3): lo usa el personal de
 * servicio para ver qué hay que preparar.
 */
public record FiltroApartamentos(Set<Caracteristica> caracteristicas, EstadoOperativo estadoOperativo) {

    public static final FiltroApartamentos SIN_FILTRO = new FiltroApartamentos(Set.of(), null);

    public FiltroApartamentos {
        EnumSet<Caracteristica> copia = EnumSet.noneOf(Caracteristica.class);
        if (caracteristicas != null) {
            copia.addAll(caracteristicas);
        }
        caracteristicas = Set.copyOf(copia);
    }

    /** Solo por características (L-06). */
    public FiltroApartamentos(Set<Caracteristica> caracteristicas) {
        this(caracteristicas, null);
    }

    public static FiltroApartamentos de(Collection<Caracteristica> caracteristicas) {
        return new FiltroApartamentos(caracteristicas == null ? Set.of() : Set.copyOf(caracteristicas), null);
    }

    public static FiltroApartamentos de(Collection<Caracteristica> caracteristicas, EstadoOperativo estadoOperativo) {
        return new FiltroApartamentos(caracteristicas == null ? Set.of() : Set.copyOf(caracteristicas), estadoOperativo);
    }
}
