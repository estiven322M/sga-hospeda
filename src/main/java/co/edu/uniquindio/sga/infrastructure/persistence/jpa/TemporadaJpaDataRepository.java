package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TemporadaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TemporadaJpaDataRepository extends JpaRepository<TemporadaEntity, String> {

    /** Una sola consulta trae las temporadas con sus periodos (evita un SELECT por temporada). */
    @Query("select distinct t from TemporadaEntity t left join fetch t.periodos order by t.nombre")
    List<TemporadaEntity> listarConPeriodos();
}
