package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AlojamientoEntity;

@Component
public class AlojamientoPersistenceMapper {

    public AlojamientoEntity aEntidad(Alojamiento alojamiento) {
        return new AlojamientoEntity(alojamiento.getNombre(), alojamiento.getDescripcion(), alojamiento.getCiudad(),
                alojamiento.getDireccion(), alojamiento.getUbicacion().latitud(), alojamiento.getUbicacion().longitud(),
                alojamiento.getHoraEntrada(), alojamiento.getHoraSalida(), alojamiento.getNormasConvivencia(),
                alojamiento.getPoliticaMascotas());
    }

    public Alojamiento aDominio(AlojamientoEntity entidad) {
        return new Alojamiento(entidad.getNombre(), entidad.getDescripcion(), entidad.getCiudad(), entidad.getDireccion(),
                new Ubicacion(entidad.getLatitud(), entidad.getLongitud()), entidad.getHoraEntrada(),
                entidad.getHoraSalida(), entidad.getNormasConvivencia(), entidad.getPoliticaMascotas());
    }
}
