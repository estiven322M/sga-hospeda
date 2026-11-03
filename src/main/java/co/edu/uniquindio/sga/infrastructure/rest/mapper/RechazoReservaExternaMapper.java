package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.RechazoReservaExternaResponse;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;

/** Dominio → respuesta de los rechazos de reservas externas. */
@Mapper(componentModel = "spring")
public interface RechazoReservaExternaMapper extends ConversionesDominio {

    @Mapping(target = "identificacionApartamento", source = "apartamento")
    @Mapping(target = "fechaEntrada", source = "estancia.fechaEntrada")
    @Mapping(target = "fechaSalida", source = "estancia.fechaSalida")
    RechazoReservaExternaResponse aResponse(RechazoReservaExterna rechazo);
}
