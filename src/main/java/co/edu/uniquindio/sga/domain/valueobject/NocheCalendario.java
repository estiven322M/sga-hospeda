package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Una noche del calendario de un apartamento. Si está reservada, trae el código y el estado de la reserva (quién puede
 * verlos lo decide la capa de aplicación según el rol); si no, ambos son null.
 */
public record NocheCalendario(LocalDate noche, EstadoNoche estado, CodigoReserva reserva, EstadoReserva estadoReserva) {

    public NocheCalendario {
        if (noche == null || estado == null) {
            throw new ReglaDominioException("Una noche del calendario requiere su fecha y su estado");
        }
        if (estado == EstadoNoche.RESERVADA && (reserva == null || estadoReserva == null)) {
            throw new ReglaDominioException("Una noche reservada debe indicar la reserva y su estado");
        }
        if (estado != EstadoNoche.RESERVADA && (reserva != null || estadoReserva != null)) {
            throw new ReglaDominioException("Solo una noche reservada indica una reserva");
        }
    }

    public static NocheCalendario libre(LocalDate noche) {
        return new NocheCalendario(noche, EstadoNoche.LIBRE, null, null);
    }

    public static NocheCalendario bloqueada(LocalDate noche) {
        return new NocheCalendario(noche, EstadoNoche.BLOQUEADA, null, null);
    }

    public static NocheCalendario reservada(LocalDate noche, CodigoReserva reserva, EstadoReserva estadoReserva) {
        return new NocheCalendario(noche, EstadoNoche.RESERVADA, reserva, estadoReserva);
    }
}
