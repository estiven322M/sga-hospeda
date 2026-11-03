package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Traduce los criterios opcionales del dominio a una sola consulta parametrizada (campo nulo = sin filtro). */
public final class ReservaEspecificaciones {

    private ReservaEspecificaciones() {
    }

    public static Specification<ReservaEntity> segun(FiltroReservas filtro) {
        return (reserva, consulta, constructor) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.entradaDesde() != null) {
                condiciones.add(constructor.greaterThanOrEqualTo(
                        reserva.get("estancia").<LocalDate>get("fechaEntrada"), filtro.entradaDesde()));
            }
            if (filtro.entradaHasta() != null) {
                condiciones.add(constructor.lessThanOrEqualTo(
                        reserva.get("estancia").<LocalDate>get("fechaEntrada"), filtro.entradaHasta()));
            }
            if (filtro.estado() != null) {
                condiciones.add(constructor.equal(reserva.get("estado"), filtro.estado()));
            }
            if (filtro.apartamento() != null) {
                condiciones.add(constructor.equal(reserva.get("apartamentoId"), filtro.apartamento().valor()));
            }
            if (filtro.canal() != null) {
                condiciones.add(constructor.equal(reserva.get("canalOrigen"), filtro.canal()));
            }
            if (filtro.titular() != null) {
                condiciones.add(constructor.equal(reserva.get("titularDocumento"), filtro.titular().numero()));
            }
            return constructor.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
