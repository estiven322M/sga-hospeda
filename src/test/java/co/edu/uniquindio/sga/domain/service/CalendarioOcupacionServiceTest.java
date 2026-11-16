package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNoche;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;
import co.edu.uniquindio.sga.testsupport.ReservasParaPruebas;

/** 7.1 y 7.5: el estado de cada noche de un apartamento (LIBRE, RESERVADA o BLOQUEADA) y el máximo de noches. */
class CalendarioOcupacionServiceTest {

    private static final IdentificacionApartamento APT = new IdentificacionApartamento("APT-101");

    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final BloqueoRepository bloqueos = mock(BloqueoRepository.class);
    private final ConfiguracionAlojamiento configuracion = mock(ConfiguracionAlojamiento.class);
    private CalendarioOcupacionService servicio;

    @BeforeEach
    void setUp() {
        when(configuracion.calendarioMaximoNoches()).thenReturn(62);
        servicio = new CalendarioOcupacionService(reservas, bloqueos, configuracion);
    }

    @Test
    void cadaNocheTieneSuEstadoYLaNocheDeLaSalidaNoSeOcupa() {
        // reserva del 12 al 14 (noches 12 y 13) y bloqueo del 16 al 18 (noches 16 y 17)
        Reserva reserva = ReservasParaPruebas.confirmada("RES-2026-00001", "APT-101", LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 14), 1);
        when(reservas.buscarActivasPorApartamento(APT)).thenReturn(List.of(reserva));
        when(bloqueos.buscarVigentesPorApartamento(APT)).thenReturn(List.of(
                new Bloqueo(APT, new Estancia(LocalDate.of(2026, 12, 16), LocalDate.of(2026, 12, 18)), "Mantenimiento")));

        List<NocheCalendario> calendario = servicio.construir(APT, new Estancia(LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 19)));

        assertEquals(8, calendario.size());
        assertEquals(EstadoNoche.LIBRE, calendario.get(0).estado());      // 11
        assertEquals(EstadoNoche.RESERVADA, calendario.get(1).estado());  // 12
        assertEquals("RES-2026-00001", calendario.get(1).reserva().valor());
        assertEquals(EstadoReserva.CONFIRMADA, calendario.get(1).estadoReserva());
        assertEquals(EstadoNoche.RESERVADA, calendario.get(2).estado());  // 13
        assertEquals(EstadoNoche.LIBRE, calendario.get(3).estado());      // 14: la noche de la salida queda libre
        assertEquals(EstadoNoche.LIBRE, calendario.get(4).estado());      // 15
        assertEquals(EstadoNoche.BLOQUEADA, calendario.get(5).estado());  // 16
        assertEquals(EstadoNoche.BLOQUEADA, calendario.get(6).estado());  // 17
        assertEquals(EstadoNoche.LIBRE, calendario.get(7).estado());      // 18
        assertEquals(null, calendario.get(5).reserva());
    }

    @Test
    void unApartamentoSinReservasNiBloqueosTieneTodasLasNochesLibres() {
        when(reservas.buscarActivasPorApartamento(APT)).thenReturn(List.of());
        when(bloqueos.buscarVigentesPorApartamento(APT)).thenReturn(List.of());

        List<NocheCalendario> calendario = servicio.construir(APT, new Estancia(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4)));

        assertEquals(3, calendario.size());
        assertEquals(List.of(EstadoNoche.LIBRE), calendario.stream().map(NocheCalendario::estado).distinct().toList());
    }

    @Test
    void elRangoAdmiteExactamenteElMaximoDeNochesYRechazaUnaMas() {
        when(reservas.buscarActivasPorApartamento(APT)).thenReturn(List.of());
        when(bloqueos.buscarVigentesPorApartamento(APT)).thenReturn(List.of());
        LocalDate desde = LocalDate.of(2026, 12, 1);

        assertEquals(62, servicio.construir(APT, new Estancia(desde, desde.plusDays(62))).size());

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> servicio.construir(APT, new Estancia(desde, desde.plusDays(63))));
        assertEquals("El calendario admite como máximo 62 noches; se pidieron 63", e.getMessage());
    }

    @Test
    void unRangoInvertidoLoRechazaLaEstancia() {
        LocalDate desde = LocalDate.of(2026, 12, 10);
        assertThrows(ReglaDominioException.class, () -> new Estancia(desde, desde));
    }

    @Test
    void unaNocheReservadaDebeIndicarLaReservaYUnaLibreNo() {
        LocalDate noche = LocalDate.of(2026, 12, 1);
        assertThrows(ReglaDominioException.class, () -> new NocheCalendario(noche, EstadoNoche.RESERVADA, null, null));
        assertThrows(ReglaDominioException.class, () -> new NocheCalendario(noche, EstadoNoche.LIBRE,
                new co.edu.uniquindio.sga.domain.valueobject.CodigoReserva("RES-2026-00001"), EstadoReserva.PENDIENTE));
    }
}
