package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PeriodoEmbeddable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TemporadaEntity;
import org.springframework.stereotype.Component;

@Component
public class TemporadaPersistenceMapper {

    public TemporadaEntity aEntidad(Temporada temporada) {
        return new TemporadaEntity(temporada.getNombre(), temporada.esBase(),
                temporada.getPeriodos().stream().map(p -> new PeriodoEmbeddable(p.desde(), p.hasta())).toList());
    }

    public Temporada aDominio(TemporadaEntity entidad) {
        return new Temporada(entidad.getNombre(), entidad.isBase(),
                entidad.getPeriodos().stream().map(p -> new Periodo(p.desde(), p.hasta())).toList());
    }
}
