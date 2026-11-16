package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EventoCanalEntity;

/** Repositorio Spring Data de la bitácora del canal. El filtro por canal y por rango de fechas es opcional (parámetro nulo = no filtra). */
public interface EventoCanalJpaDataRepository extends JpaRepository<EventoCanalEntity, Long> {

    @Query("select e from EventoCanalEntity e where (:canal is null or e.canal = :canal) "
            + "and (:desde is null or e.registradoEn >= :desde) and (:hasta is null or e.registradoEn < :hasta)")
    Page<EventoCanalEntity> buscar(@Param("canal") String canal, @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta, Pageable pageable);
}
