package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.CalendarioResponse;
import co.edu.uniquindio.sga.application.dto.response.CalendarioResponse.EstadoVisible;
import co.edu.uniquindio.sga.application.dto.response.CalendarioResponse.NocheResponse;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;

/**
 * Dominio → respuesta del calendario según lo que el rol puede ver (7.1): con {@code detallado} (recepción y
 * administración) cada noche trae su estado real y, si está reservada, el código y el estado de la reserva; sin él (el
 * huésped) solo se distingue LIBRE de NO_DISPONIBLE, sin revelar por qué ni de quién es la reserva.
 */
@Component
public class CalendarioMapper {

    public CalendarioResponse aResponse(String apartamento, LocalDate desde, LocalDate hasta, List<NocheCalendario> noches,
                                        boolean detallado) {
        return new CalendarioResponse(apartamento, desde, hasta,
                noches.stream().map(noche -> aNoche(noche, detallado)).toList());
    }

    private static NocheResponse aNoche(NocheCalendario noche, boolean detallado) {
        if (detallado) {
            EstadoVisible estado = switch (noche.estado()) {
                case LIBRE -> EstadoVisible.LIBRE;
                case RESERVADA -> EstadoVisible.RESERVADA;
                case BLOQUEADA -> EstadoVisible.BLOQUEADA;
            };
            return new NocheResponse(noche.noche(), estado,
                    noche.reserva() == null ? null : noche.reserva().valor(), noche.estadoReserva());
        }
        return new NocheResponse(noche.noche(),
                noche.estado() == co.edu.uniquindio.sga.domain.valueobject.EstadoNoche.LIBRE
                        ? EstadoVisible.LIBRE : EstadoVisible.NO_DISPONIBLE,
                null, null);
    }
}
