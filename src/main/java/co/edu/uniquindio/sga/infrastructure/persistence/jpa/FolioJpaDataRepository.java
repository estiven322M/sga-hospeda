package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolioJpaDataRepository extends JpaRepository<FolioEntity, String> {
}
