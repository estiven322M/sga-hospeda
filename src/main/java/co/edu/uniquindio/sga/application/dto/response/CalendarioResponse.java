package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/**
 * Calendario de un apartamento (7.1, 7.5) sobre las noches [desde, hasta). Recepción y administración ven el código y el
 * estado de la reserva de cada noche reservada; el huésped solo ve LIBRE o NO_DISPONIBLE (no se le revela por qué ni de quién).
 */
public record CalendarioResponse(
        String identificacionApartamento,
        LocalDate desde,
        LocalDate hasta,
        List<NocheResponse> noches) {

    /** Lo que se puede ver de una noche. */
    public enum EstadoVisible { LIBRE, RESERVADA, BLOQUEADA, NO_DISPONIBLE }

    /** {@code codigoReserva} y {@code estadoReserva} solo vienen para quien puede verlos (recepción y administración). */
    public record NocheResponse(LocalDate noche, EstadoVisible estado, String codigoReserva, EstadoReserva estadoReserva) {
    }
}
