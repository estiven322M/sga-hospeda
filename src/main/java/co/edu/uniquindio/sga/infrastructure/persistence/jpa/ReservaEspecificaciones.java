package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.OcupanteEmbeddable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Traduce los criterios opcionales del dominio a una sola consulta parametrizada (campo nulo = sin filtro). */
public final class ReservaEspecificaciones {

    private static final char ESCAPE = '\\';

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
            if (filtro.salidaDesde() != null) {
                condiciones.add(constructor.greaterThanOrEqualTo(
                        reserva.get("estancia").<LocalDate>get("fechaSalida"), filtro.salidaDesde()));
            }
            if (filtro.salidaHasta() != null) {
                condiciones.add(constructor.lessThanOrEqualTo(
                        reserva.get("estancia").<LocalDate>get("fechaSalida"), filtro.salidaHasta()));
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
            if (filtro.nombreTitular() != null) {
                // El nombre del titular está entre los ocupantes: se une con el ocupante cuyo documento es el del titular
                // (en una reserva el documento no se repite, así que la unión no duplica filas ni rompe la paginación).
                Join<ReservaEntity, OcupanteEmbeddable> ocupante = reserva.join("ocupantes");
                condiciones.add(constructor.equal(ocupante.get("documento"), reserva.get("titularDocumento")));
                condiciones.add(constructor.like(constructor.lower(ocupante.get("nombre")),
                        "%" + escapar(filtro.nombreTitular().toLowerCase(Locale.ROOT)) + "%", ESCAPE));
            }
            return constructor.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /** Los comodines de LIKE escritos por el usuario se toman literalmente. */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
