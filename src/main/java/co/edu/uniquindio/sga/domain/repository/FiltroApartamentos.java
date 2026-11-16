package co.edu.uniquindio.sga.domain.repository;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;

/**
 * Criterios opcionales para listar apartamentos (7.5). Sin características no se filtra; con varias, el
 * apartamento debe tener TODAS (Y lógico), igual que {@code Apartamento.tieneTodas}.
 */
public record FiltroApartamentos(Set<Caracteristica> caracteristicas) {

    public static final FiltroApartamentos SIN_FILTRO = new FiltroApartamentos(Set.of());

    public FiltroApartamentos {
        EnumSet<Caracteristica> copia = EnumSet.noneOf(Caracteristica.class);
        if (caracteristicas != null) {
            copia.addAll(caracteristicas);
        }
        caracteristicas = Set.copyOf(copia);
    }

    public static FiltroApartamentos de(Collection<Caracteristica> caracteristicas) {
        return new FiltroApartamentos(caracteristicas == null ? Set.of() : Set.copyOf(caracteristicas));
    }
}
