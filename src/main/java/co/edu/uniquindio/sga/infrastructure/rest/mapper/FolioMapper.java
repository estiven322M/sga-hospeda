package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.FolioResponse;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.Ajuste;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/** Dominio → respuesta del folio. El saldo lo calcula el Folio; el mapper solo lo convierte a pesos. */
@Mapper(componentModel = "spring")
public interface FolioMapper extends ConversionesDominio {

    @Mapping(target = "codigoReserva", source = "reserva")
    @Mapping(target = "cargos", expression = "java(folio.obtenerCargos().stream().map(this::aCargo).toList())")
    @Mapping(target = "ajustes", expression = "java(folio.obtenerAjustes().stream().map(this::aAjuste).toList())")
    @Mapping(target = "pagos", expression = "java(folio.obtenerPagos().stream().map(this::aPago).toList())")
    @Mapping(target = "saldo", expression = "java(aPesos(folio.saldo()))")
    @Mapping(target = "moneda", constant = "COP")
    @Mapping(target = "cerrado", expression = "java(folio.estaCerrado())")
    FolioResponse aResponse(Folio folio);

    FolioResponse.CargoResponse aCargo(Cargo cargo);

    FolioResponse.AjusteResponse aAjuste(Ajuste ajuste);

    FolioResponse.PagoResponse aPago(Pago pago);
}
