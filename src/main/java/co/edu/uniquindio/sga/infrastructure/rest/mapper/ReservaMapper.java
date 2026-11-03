package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.edu.uniquindio.sga.application.dto.response.ApartamentoDisponibleResponse;
import co.edu.uniquindio.sga.application.dto.response.CargoNocheResponse;
import co.edu.uniquindio.sga.application.dto.response.CotizacionResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoReservaResponse;
import co.edu.uniquindio.sga.application.dto.response.OcupanteResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaResumenResponse;
import co.edu.uniquindio.sga.application.usecase.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.EventoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;

/**
 * Dominio → respuesta de reservas, cotizaciones y disponibilidad (MapStruct genera la implementación en
 * compilación). Dinero viaja como entero de pesos (ConversionesDominio); no se calcula nada aquí: los
 * totales y subtotales los entrega el dominio.
 */
@Mapper(componentModel = "spring")
public interface ReservaMapper extends ConversionesDominio {

    @Mapping(target = "identificacionApartamento", source = "apartamento")
    @Mapping(target = "fechaEntrada", source = "estancia.fechaEntrada")
    @Mapping(target = "fechaSalida", source = "estancia.fechaSalida")
    @Mapping(target = "noches", expression = "java(reserva.getEstancia().noches())")
    @Mapping(target = "valorTotal", expression = "java(aPesos(reserva.getValor().total()))")
    @Mapping(target = "moneda", constant = "COP")
    @Mapping(target = "desglose", source = "valor.detalle")
    @Mapping(target = "versionPolitica", source = "politica.numero")
    ReservaDetalleResponse aDetalle(Reserva reserva);

    @Mapping(target = "identificacionApartamento", source = "apartamento")
    @Mapping(target = "fechaEntrada", source = "estancia.fechaEntrada")
    @Mapping(target = "fechaSalida", source = "estancia.fechaSalida")
    @Mapping(target = "nombreTitular", source = "titular.nombre")
    @Mapping(target = "totalOcupantes", expression = "java(reserva.getOcupantes().size())")
    @Mapping(target = "valorTotal", expression = "java(aPesos(reserva.getValor().total()))")
    ReservaResumenResponse aResumen(Reserva reserva);

    OcupanteResponse aOcupanteResponse(Ocupante ocupante);

    EventoReservaResponse aEvento(EventoReserva evento);

    @Mapping(target = "subtotal", expression = "java(aPesos(cargo.subtotal()))")
    CargoNocheResponse aCargoNoche(CargoNoche cargo);

    List<CargoNocheResponse> aDesglose(List<CargoNoche> detalle);

    @Mapping(target = "identificacion", source = "apartamento.identificacion")
    @Mapping(target = "nombre", source = "apartamento.nombre")
    @Mapping(target = "dormitorios", source = "apartamento.dormitorios")
    @Mapping(target = "capacidad", source = "apartamento.capacidad")
    @Mapping(target = "total", expression = "java(aPesos(disponible.cotizacion().total()))")
    @Mapping(target = "moneda", constant = "COP")
    @Mapping(target = "desglose", source = "cotizacion.detalle")
    ApartamentoDisponibleResponse aDisponible(ApartamentoDisponible disponible);

    default CotizacionResponse aCotizacion(IdentificacionApartamento apartamento, Estancia estancia,
                                           ValorCongelado valor) {
        return new CotizacionResponse(apartamento.valor(), estancia.fechaEntrada(), estancia.fechaSalida(),
                estancia.noches(), aDesglose(valor.detalle()), aPesos(valor.total()), MONEDA);
    }
}
