package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.TemporadaPersistenceMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class TemporadaJpaRepository implements TemporadaRepository {

    private final TemporadaJpaDataRepository datos;
    private final TemporadaPersistenceMapper mapper;

    public TemporadaJpaRepository(TemporadaJpaDataRepository datos, TemporadaPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    /** Se une a la transacción del caso de uso y mapea las colecciones del agregado guardado dentro de ella. */
    @Override
    @Transactional
    public Temporada guardar(Temporada temporada) {
        return mapper.aDominio(datos.save(mapper.aEntidad(temporada)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Temporada> listarTodas() {
        return datos.listarConPeriodos().stream().map(mapper::aDominio).toList();
    }
}
