package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.FiltroApartamentos;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Traduce el filtro del dominio a una consulta parametrizada: una condición "es miembro de" por cada característica
 * pedida, unidas con Y, de modo que el apartamento debe tenerlas todas; y, si se pide, el estado operativo.
 * Sin criterios no agrega condiciones. Es un filtro de búsqueda, no una regla de negocio: la regla equivalente de las
 * características vive en Apartamento.tieneTodas.
 */
public final class ApartamentoEspecificaciones {

    private ApartamentoEspecificaciones() {
    }

    public static Specification<ApartamentoEntity> segun(FiltroApartamentos filtro) {
        return (apartamento, consulta, constructor) -> {
            List<Predicate> condiciones = new ArrayList<>();
            for (Caracteristica caracteristica : filtro.caracteristicas()) {
                condiciones.add(constructor.isMember(caracteristica,
                        apartamento.<Set<Caracteristica>>get("caracteristicas")));
            }
            if (filtro.estadoOperativo() != null) {
                condiciones.add(constructor.equal(apartamento.get("estadoOperativo"), filtro.estadoOperativo()));
            }
            return constructor.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
