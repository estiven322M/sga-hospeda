package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.RechazoReservaExternaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechazoReservaExternaJpaDataRepository extends JpaRepository<RechazoReservaExternaEntity, Long> {
}
