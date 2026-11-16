package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ServicioAdicionalEntity;

@Component
public class ServicioAdicionalPersistenceMapper {

    public ServicioAdicionalEntity aEntidad(ServicioAdicional servicio) {
        return new ServicioAdicionalEntity(servicio.getCodigo(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.generaCargo(), servicio.getValor().valor().longValueExact(), servicio.isActivo());
    }

    public ServicioAdicional aDominio(ServicioAdicionalEntity entidad) {
        return ServicioAdicional.reconstruir(entidad.getCodigo(), entidad.getNombre(), entidad.getDescripcion(),
                entidad.isGeneraCargo(), Dinero.de(entidad.getValor()), entidad.isActivo());
    }
}
