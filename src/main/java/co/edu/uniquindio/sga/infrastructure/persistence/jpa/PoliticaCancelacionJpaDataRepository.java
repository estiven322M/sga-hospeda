package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PoliticaCancelacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PoliticaCancelacionJpaDataRepository extends JpaRepository<PoliticaCancelacionEntity, Integer> {

    Optional<PoliticaCancelacionEntity> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDescNumeroVersionDesc(
            LocalDate fecha);

    Optional<PoliticaCancelacionEntity> findFirstByOrderByNumeroVersionDesc();
}
