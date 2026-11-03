package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.BloqueoResponse;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;

/** Dominio → respuesta de bloqueos. El rango [inicio, fin) sale del objeto de valor Estancia. */
@Mapper(componentModel = "spring")
public interface BloqueoMapper extends ConversionesDominio {

    @Mapping(target = "identificacionApartamento", source = "apartamento")
    @Mapping(target = "fechaInicio", source = "rango.fechaEntrada")
    @Mapping(target = "fechaFin", source = "rango.fechaSalida")
    BloqueoResponse aResponse(Bloqueo bloqueo);
}
