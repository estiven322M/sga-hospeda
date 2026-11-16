package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.domain.entity.Apartamento;

/** Dominio → respuesta de apartamentos. */
@Mapper(componentModel = "spring")
public interface ApartamentoMapper extends ConversionesDominio {

    @Mapping(target = "imagenPrincipal", expression = "java(urlImagenPrincipal(apartamento))")
    @Mapping(target = "imagenes", expression = "java(imagenesDe(apartamento))")
    ApartamentoResponse aResponse(Apartamento apartamento);
}
