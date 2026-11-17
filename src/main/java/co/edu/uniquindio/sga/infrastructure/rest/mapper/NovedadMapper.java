package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.NovedadResponse;
import co.edu.uniquindio.sga.domain.entity.Novedad;

/** Dominio → respuesta de novedades. La identificación del apartamento viaja como texto. */
@Mapper(componentModel = "spring")
public interface NovedadMapper extends ConversionesDominio {

    @Mapping(target = "identificacionApartamento", source = "apartamento")
    NovedadResponse aResponse(Novedad novedad);
}
