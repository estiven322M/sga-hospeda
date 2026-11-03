package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PoliticaCancelacionEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TramoRetencionEmbeddable;
import org.springframework.stereotype.Component;

@Component
public class PoliticaCancelacionPersistenceMapper {

    public PoliticaCancelacionEntity aEntidad(PoliticaCancelacion politica) {
        return new PoliticaCancelacionEntity(politica.getVersion().numero(), politica.getVigenteDesde(),
                politica.getTramos().stream()
                        .map(t -> new TramoRetencionEmbeddable(t.horasAntelacionMinima(), t.porcentajeRetencion()))
                        .toList());
    }

    public PoliticaCancelacion aDominio(PoliticaCancelacionEntity entidad) {
        return new PoliticaCancelacion(new VersionPolitica(entidad.getNumeroVersion()), entidad.getVigenteDesde(),
                entidad.getTramos().stream()
                        .map(t -> new TramoRetencion(t.horasAntelacionMinima(), t.porcentajeRetencion()))
                        .toList());
    }
}
