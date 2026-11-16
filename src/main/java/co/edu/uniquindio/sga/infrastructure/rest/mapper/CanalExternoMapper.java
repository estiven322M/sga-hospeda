package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.CanalVentaCreadoResponse;
import co.edu.uniquindio.sga.application.dto.response.CanalVentaResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaExternaResponse;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.entity.Reserva;

/** Dominio → respuestas del canal externo. Nunca incluye el hash de la credencial; el texto de la credencial solo sale al crear el canal. */
@Component
public class CanalExternoMapper {

    public CanalVentaResponse aResponse(CanalVenta canal) {
        return new CanalVentaResponse(canal.getCodigo(), canal.getNombre(), canal.getTipo(), canal.isActivo());
    }

    public CanalVentaCreadoResponse aCreado(CanalVenta canal, String credencial) {
        return new CanalVentaCreadoResponse(canal.getCodigo(), canal.getNombre(), canal.getTipo(), canal.isActivo(), credencial);
    }

    public EventoCanalResponse aResponse(EventoCanal evento) {
        return new EventoCanalResponse(evento.registradoEn(), evento.canal(), evento.operacion(), evento.identificadorExterno(),
                evento.resultado(), evento.codigoHttp(), evento.detalle());
    }

    /** La reserva como la ve el canal: con SU identificador (sin el código del canal por delante). */
    public ReservaExternaResponse aReservaExterna(CanalVenta canal, Reserva reserva) {
        return new ReservaExternaResponse(reserva.getCodigo().valor(), canal.identificadorPropio(reserva.getIdentificadorExterno()),
                reserva.getApartamento().valor(), reserva.getEstancia().fechaEntrada(), reserva.getEstancia().fechaSalida(),
                reserva.getEstado(), reserva.getValor().total().valor().longValueExact(), ConversionesDominio.MONEDA);
    }
}
