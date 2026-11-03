package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EstanciaEmbeddable;
import org.springframework.stereotype.Component;

@Component
public class EstanciaPersistenceMapper {

    public EstanciaEmbeddable aEmbeddable(Estancia estancia) {
        return new EstanciaEmbeddable(estancia.fechaEntrada(), estancia.fechaSalida());
    }

    public Estancia aDominio(EstanciaEmbeddable embeddable) {
        return new Estancia(embeddable.fechaEntrada(), embeddable.fechaSalida());
    }
}
