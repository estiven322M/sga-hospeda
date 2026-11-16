package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.ServicioAdicionalResponse;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;

/** Dominio → respuesta de servicios adicionales. El dinero viaja como entero de pesos COP. */
@Component
public class ServicioAdicionalMapper {

    public ServicioAdicionalResponse aResponse(ServicioAdicional servicio) {
        return new ServicioAdicionalResponse(servicio.getCodigo(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.generaCargo(), servicio.getValor().valor().longValueExact(), ConversionesDominio.MONEDA,
                servicio.isActivo());
    }
}
