package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaJpaDataRepository
        extends JpaRepository<ReservaEntity, String>, JpaSpecificationExecutor<ReservaEntity> {

    @EntityGraph(attributePaths = "ocupantes")
    List<ReservaEntity> findByApartamentoIdAndEstadoIn(String apartamentoId, Collection<EstadoReserva> estados);

    @EntityGraph(attributePaths = "ocupantes")
    Optional<ReservaEntity> findByCodigo(String codigo);

    @EntityGraph(attributePaths = "ocupantes")
    List<ReservaEntity> findByEstadoAndCreadaEnBefore(EstadoReserva estado, LocalDateTime limite);
}
