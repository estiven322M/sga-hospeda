package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.FiltroApartamentos;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * Traduce el filtro del dominio a una consulta parametrizada: una condición "es miembro de" por cada característica
 * pedida, unidas con Y, de modo que el apartamento debe tenerlas todas. Sin características no agrega condiciones.
 * Es un filtro de búsqueda, no una regla de negocio: la regla equivalente vive en Apartamento.tieneTodas.
 */
public final class ApartamentoEspecificaciones {

    private ApartamentoEspecificaciones() {
    }

    public static Specification<ApartamentoEntity> segun(FiltroApartamentos filtro) {
        return (apartamento, consulta, constructor) -> {
            Predicate[] condiciones = filtro.caracteristicas().stream()
                    .map(caracteristica -> constructor.isMember(caracteristica,
                            apartamento.<Set<Caracteristica>>get("caracteristicas")))
                    .toArray(Predicate[]::new);
            return constructor.and(condiciones);
        };
    }
}
