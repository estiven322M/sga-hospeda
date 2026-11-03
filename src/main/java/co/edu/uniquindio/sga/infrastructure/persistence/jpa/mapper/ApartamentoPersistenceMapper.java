package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import org.springframework.stereotype.Component;

@Component
public class ApartamentoPersistenceMapper {

    public ApartamentoEntity aEntidad(Apartamento apartamento) {
        return new ApartamentoEntity(apartamento.getIdentificacion().valor(), apartamento.getNombre(),
                apartamento.getDormitorios(), apartamento.getCapacidad(), apartamento.getEstadoOperativo(),
                apartamento.isActivo());
    }

    public Apartamento aDominio(ApartamentoEntity entidad) {
        return Apartamento.reconstruir(new IdentificacionApartamento(entidad.getIdentificacion()),
                entidad.getNombre(), entidad.getDormitorios(), entidad.getCapacidad(),
                entidad.getEstadoOperativo(), entidad.isActivo());
    }
}
