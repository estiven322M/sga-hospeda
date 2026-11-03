package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;

import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.domain.entity.Apartamento;

/** Dominio → respuesta de apartamentos. */
@Mapper(componentModel = "spring")
public interface ApartamentoMapper extends ConversionesDominio {

    ApartamentoResponse aResponse(Apartamento apartamento);
}
