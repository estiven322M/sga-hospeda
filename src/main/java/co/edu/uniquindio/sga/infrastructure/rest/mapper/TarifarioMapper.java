package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.PoliticaCancelacionResponse;
import co.edu.uniquindio.sga.application.dto.response.TarifaResponse;
import co.edu.uniquindio.sga.application.dto.response.TemporadaResponse;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;

/** Dominio → respuesta de temporadas, tarifas y política de cancelación. */
@Mapper(componentModel = "spring")
public interface TarifarioMapper extends ConversionesDominio {

    @Mapping(target = "base", expression = "java(temporada.esBase())")
    @Mapping(target = "periodos", source = "periodos")
    TemporadaResponse aTemporada(Temporada temporada);

    TemporadaResponse.PeriodoResponse aPeriodo(Periodo periodo);

    @Mapping(target = "moneda", constant = "COP")
    TarifaResponse aTarifa(Tarifa tarifa);

    @Mapping(target = "version", source = "version.numero")
    PoliticaCancelacionResponse aPolitica(PoliticaCancelacion politica);

    PoliticaCancelacionResponse.TramoResponse aTramo(TramoRetencion tramo);
}
