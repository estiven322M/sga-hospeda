package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.NovedadEntity;

/** Repositorio Spring Data de las novedades. El orden (más reciente primero) lo fija el adaptador en el Pageable. */
public interface NovedadJpaDataRepository extends JpaRepository<NovedadEntity, Long> {

    Page<NovedadEntity> findByApartamento(String apartamento, Pageable pageable);
}
