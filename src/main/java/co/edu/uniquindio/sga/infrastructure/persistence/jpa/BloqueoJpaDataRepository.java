package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloqueoJpaDataRepository extends JpaRepository<BloqueoEntity, Long> {

    List<BloqueoEntity> findByApartamentoIdOrderByEstanciaFechaEntradaAscIdAsc(String apartamentoId);

    Page<BloqueoEntity> findByApartamentoId(String apartamentoId, Pageable pageable);
}
