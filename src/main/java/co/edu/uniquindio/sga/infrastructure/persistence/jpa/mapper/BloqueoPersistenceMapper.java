package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import org.springframework.stereotype.Component;

@Component
public class BloqueoPersistenceMapper {

    private final EstanciaPersistenceMapper estancias;

    public BloqueoPersistenceMapper(EstanciaPersistenceMapper estancias) {
        this.estancias = estancias;
    }

    public BloqueoEntity aEntidad(Bloqueo bloqueo) {
        return new BloqueoEntity(bloqueo.getApartamento().valor(), estancias.aEmbeddable(bloqueo.getRango()),
                bloqueo.getMotivo());
    }

    public Bloqueo aDominio(BloqueoEntity entidad) {
        return new Bloqueo(new IdentificacionApartamento(entidad.getApartamentoId()),
                estancias.aDominio(entidad.getEstancia()), entidad.getMotivo());
    }
}
