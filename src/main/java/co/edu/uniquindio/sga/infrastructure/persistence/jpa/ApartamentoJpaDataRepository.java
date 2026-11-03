package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApartamentoJpaDataRepository extends JpaRepository<ApartamentoEntity, String> {

    List<ApartamentoEntity> findByActivoTrueOrderByIdentificacion();

    Page<ApartamentoEntity> findByActivoTrue(Pageable pageable);
}
