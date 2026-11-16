package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import co.edu.uniquindio.sga.testsupport.ReservasParaPruebas;

/** 7.3: al bajar la capacidad se advierte de las reservas activas que quedan por encima, sin tocarlas. */
class ControlCapacidadServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final ControlCapacidadService servicio = new ControlCapacidadService(reservas);
    private final IdentificacionApartamento id = new IdentificacionApartamento("APT-101");

    @Test
    void listaLasReservasActivasConMasOcupantesQueLaNuevaCapacidad() {
        Apartamento apartamento = ApartamentoDePrueba.nuevo(id, "Vista", 2, 4);
        Reserva de4 = ReservasParaPruebas.confirmada("RES-2026-00001", "APT-101", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), 4);
        Reserva de2 = ReservasParaPruebas.pendiente("RES-2026-00002", "APT-101", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 2);
        Reserva de3 = ReservasParaPruebas.pendiente("RES-2026-00003", "APT-101", LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22), 3);
        when(reservas.buscarActivasPorApartamento(id)).thenReturn(List.of(de4, de2, de3));

        apartamento.cambiarCapacidad(3); // baja de 4 a 3: la de 4 personas queda por encima; la de 3 y la de 2 no

        List<Reserva> afectadas = servicio.reservasPorEncimaDeLaCapacidad(apartamento, HOY);
        assertEquals(List.of(de4), afectadas);
        // no se modifica ninguna reserva: sigue con sus 4 ocupantes
        assertEquals(4, de4.getOcupantes().size());
    }

    @Test
    void siNingunaReservaSuperaLaCapacidadLaListaEstaVacia() {
        Apartamento apartamento = ApartamentoDePrueba.nuevo(id, "Vista", 2, 4);
        Reserva de2 = ReservasParaPruebas.pendiente("RES-2026-00002", "APT-101", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 2);
        when(reservas.buscarActivasPorApartamento(id)).thenReturn(List.of(de2));

        assertTrue(servicio.reservasPorEncimaDeLaCapacidad(apartamento, HOY).isEmpty());
    }

    @Test
    void unaReservaQueTerminaHoyYaNoRetieneLaCapacidadYNoSeAvisa() {
        Apartamento apartamento = ApartamentoDePrueba.nuevo(id, "Vista", 2, 2);
        Reserva vieja = ReservasParaPruebas.confirmada("RES-2026-00004", "APT-101", LocalDate.of(2026, 11, 14), HOY, 4); // sale hoy
        when(reservas.buscarActivasPorApartamento(id)).thenReturn(List.of(vieja));

        assertTrue(servicio.reservasPorEncimaDeLaCapacidad(apartamento, HOY).isEmpty());
    }
}
