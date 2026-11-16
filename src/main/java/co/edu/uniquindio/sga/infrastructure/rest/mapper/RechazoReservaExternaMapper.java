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
    @Mapping(target = "canal", expression = "java(canalDe(rechazo))")
    @Mapping(target = "revisado", expression = "java(rechazo.estaRevisado())")
    @Mapping(target = "notaRevision", source = "revision.nota")
    @Mapping(target = "revisadoPor", source = "revision.revisadoPor")
    @Mapping(target = "revisadoEn", source = "revision.revisadaEn")
    RechazoReservaExternaResponse aResponse(RechazoReservaExterna rechazo);

    /** El código del canal: la parte del identificador guardado antes de ":" (nulo si el identificador no lleva canal). */
    default String canalDe(RechazoReservaExterna rechazo) {
        String valor = rechazo.getIdentificadorExterno().valor();
        int separador = valor.indexOf(':');
        return separador > 0 ? valor.substring(0, separador) : null;
    }
}
