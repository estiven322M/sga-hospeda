package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** RN-08 (ciclo de vida válido) y RN-12 (solo las reservas activas retienen noches). */
class EstadoReservaTest {

    @Test
    void soloSePermitenLasTransicionesDeLaSeccion8RN08() {
        assertEquals(Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA), destinosDe(EstadoReserva.PENDIENTE));
        assertEquals(Set.of(EstadoReserva.EN_CURSO, EstadoReserva.CANCELADA, EstadoReserva.NO_SHOW),
                destinosDe(EstadoReserva.CONFIRMADA));
        assertEquals(Set.of(EstadoReserva.FINALIZADA), destinosDe(EstadoReserva.EN_CURSO));
    }

    @Test
    void losEstadosTerminalesNoTransicionanANingunLado() {
        for (EstadoReserva terminal : new EstadoReserva[] {
                EstadoReserva.FINALIZADA, EstadoReserva.CANCELADA, EstadoReserva.NO_SHOW}) {
            assertTrue(terminal.esTerminal());
            assertTrue(destinosDe(terminal).isEmpty(), terminal + " no debería tener transiciones");
        }
    }

    @Test
    void unaReservaEnCursoNoPuedeCancelarseNiVolverAConfirmadaRN08() {
        assertFalse(EstadoReserva.EN_CURSO.puedeTransicionarA(EstadoReserva.CANCELADA));
        assertFalse(EstadoReserva.EN_CURSO.puedeTransicionarA(EstadoReserva.CONFIRMADA));
        assertFalse(EstadoReserva.PENDIENTE.puedeTransicionarA(EstadoReserva.EN_CURSO));
    }

    @Test
    void soloLasReservasActivasRetienenDisponibilidadRN12() {
        assertEquals(Set.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO),
                EnumSet.allOf(EstadoReserva.class).stream().filter(EstadoReserva::retieneDisponibilidad)
                        .collect(java.util.stream.Collectors.toSet()));
        // al cancelar, vencer o finalizar, la reserva deja de retener noches de inmediato
        assertFalse(EstadoReserva.CANCELADA.retieneDisponibilidad());
        assertFalse(EstadoReserva.NO_SHOW.retieneDisponibilidad());
        assertFalse(EstadoReserva.FINALIZADA.retieneDisponibilidad());
    }

    private Set<EstadoReserva> destinosDe(EstadoReserva origen) {
        return EnumSet.allOf(EstadoReserva.class).stream().filter(origen::puedeTransicionarA)
                .collect(java.util.stream.Collectors.toSet());
    }
}
