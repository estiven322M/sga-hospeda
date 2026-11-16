package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ServicioAdicionalEntity;

public interface ServicioAdicionalJpaDataRepository extends JpaRepository<ServicioAdicionalEntity, String> {

    Page<ServicioAdicionalEntity> findByActivoTrue(Pageable pagina);
}
