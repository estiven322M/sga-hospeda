package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.OcupanteEmbeddable;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class OcupantePersistenceMapper {

    private final Clock reloj;

    public OcupantePersistenceMapper(Clock reloj) {
        this.reloj = reloj;
    }

    public OcupanteEmbeddable aEmbeddable(Ocupante ocupante) {
        return new OcupanteEmbeddable(ocupante.getDocumento().numero(), ocupante.getNombre(),
                ocupante.getFechaNacimiento());
    }

    /** El dominio pide la fecha actual para validar el nacimiento; al restaurar se usa la de hoy. */
    public Ocupante aDominio(OcupanteEmbeddable embeddable) {
        return new Ocupante(new DocumentoIdentidad(embeddable.documento()), embeddable.nombre(),
                embeddable.fechaNacimiento(), LocalDate.now(reloj));
    }
}
