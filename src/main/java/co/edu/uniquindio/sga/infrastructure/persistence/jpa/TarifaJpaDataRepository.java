package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifaJpaDataRepository extends JpaRepository<TarifaEntity, TarifaId> {

    List<TarifaEntity> findByIdApartamentoIdOrderByIdTemporada(String apartamentoId);
}
