package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.RechazoReservaExternaEntity;
import org.springframework.stereotype.Component;

@Component
public class RechazoReservaExternaPersistenceMapper {

    private final EstanciaPersistenceMapper estancias;

    public RechazoReservaExternaPersistenceMapper(EstanciaPersistenceMapper estancias) {
        this.estancias = estancias;
    }

    public RechazoReservaExternaEntity aEntidad(RechazoReservaExterna rechazo) {
        return new RechazoReservaExternaEntity(rechazo.getCanalOrigen(), rechazo.getIdentificadorExterno().valor(),
                rechazo.getApartamento().valor(), estancias.aEmbeddable(rechazo.getEstancia()), rechazo.getMotivo(),
                rechazo.getRechazadaEn());
    }

    public RechazoReservaExterna aDominio(RechazoReservaExternaEntity entidad) {
        return new RechazoReservaExterna(entidad.getCanalOrigen(),
                new IdentificadorExterno(entidad.getIdentificadorExterno()),
                new IdentificacionApartamento(entidad.getApartamentoId()),
                estancias.aDominio(entidad.getEstancia()), entidad.getMotivo(), entidad.getRechazadaEn());
    }
}
