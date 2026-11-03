package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.TemporadaPersistenceMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class TemporadaJpaRepository implements TemporadaRepository {

    private final TemporadaJpaDataRepository datos;
    private final TemporadaPersistenceMapper mapper;

    public TemporadaJpaRepository(TemporadaJpaDataRepository datos, TemporadaPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Temporada guardar(Temporada temporada) {
        return mapper.aDominio(datos.save(mapper.aEntidad(temporada)));
    }

    @Override
    public List<Temporada> listarTodas() {
        return datos.listarConPeriodos().stream().map(mapper::aDominio).toList();
    }
}
