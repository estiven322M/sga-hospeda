package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** Confirmar exige cubrir el anticipo configurado (L-11) además de la hora estimada de llegada (RN-09). */
class ConfirmacionReservaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);

    private final ConfiguracionAlojamiento config = mock(ConfiguracionAlojamiento.class);
    private final ConfirmacionReservaService servicio = new ConfirmacionReservaService(config);
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private Reserva reserva(long porNoche, boolean conHoraLlegada) {
        Reserva r = Reserva.crear(codigo, new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(porNoche), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(porNoche), 1))),
                new VersionPolitica(1), AHORA);
        if (conHoraLlegada) {
            r.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Juan", AHORA);
        }
        return r;
    }

    private Folio folio(Reserva r) {
        return new Folio(r.getCodigo(), new Cargo("Alojamiento", r.getValor().total()));
    }

    private Pago pago(long valor) {
        return new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(valor));
    }

    @Test
    void confirmaCuandoLoPagadoCubreElAnticipo() {
        when(config.anticipoPorcentaje()).thenReturn(30);
        Reserva r = reserva(100_000, true); // total 200.000 -> anticipo 60.000
        Folio f = folio(r);
        f.registrarPago(pago(60_000));

        servicio.confirmar(r, f, "Juan", AHORA);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
    }

    @Test
    void sumaLosPagosParcialesParaCubrirElAnticipo() {
        when(config.anticipoPorcentaje()).thenReturn(30);
        Reserva r = reserva(100_000, true);
        Folio f = folio(r);
        f.registrarPago(pago(25_000));
        f.registrarPago(pago(40_000));

        servicio.confirmar(r, f, "Juan", AHORA);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
    }

    @Test
    void rechazaConfirmarSinAnticipoYLaReservaSigueEnPendiente() {
        when(config.anticipoPorcentaje()).thenReturn(30);
        Reserva r = reserva(100_000, true);
        Folio f = folio(r);
        f.registrarPago(pago(59_999));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> servicio.confirmar(r, f, "Juan", AHORA));

        assertTrue(ex.getMessage().contains("60000"));
        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
    }

    @Test
    void sinAnticipoConfiguradoConfirmaSinPagos() {
        when(config.anticipoPorcentaje()).thenReturn(0);
        Reserva r = reserva(100_000, true);

        servicio.confirmar(r, folio(r), "Juan", AHORA);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
    }

    @Test
    void elAnticipoSeRedondeaAlPeso() {
        when(config.anticipoPorcentaje()).thenReturn(30);
        // total 200.002 * 30 % = 60.000,6 -> 60.001
        assertEquals(Dinero.de(60_001), servicio.anticipoRequerido(reserva(100_001, false)));
    }

    @Test
    void seguiraExigiendoLaHoraEstimadaDeLlegadaRN09AunConAnticipoPagado() {
        when(config.anticipoPorcentaje()).thenReturn(30);
        Reserva r = reserva(100_000, false);
        Folio f = folio(r);
        f.registrarPago(pago(200_000));

        assertThrows(ReglaDominioException.class, () -> servicio.confirmar(r, f, "Juan", AHORA));
        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
    }

    @Test
    void rechazaConfirmarUnaReservaQueNoEstaPendienteSinPedirAnticipoRN08() {
        Reserva r = reserva(100_000, true);
        r.cancelar("Cambio de planes", "Juan", AHORA);

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> servicio.confirmar(r, folio(r), "Juan", AHORA));

        assertEquals("No se puede pasar de CANCELADA a CONFIRMADA", ex.getMessage());
        verifyNoInteractions(config);
    }

    @Test
    void rechazaUnFolioQueNoEsDeLaReserva() {
        Reserva r = reserva(100_000, true);
        Folio ajeno = new Folio(new CodigoReserva("RES-2026-00099"), new Cargo("Alojamiento", Dinero.de(1)));

        assertThrows(ReglaDominioException.class, () -> servicio.confirmar(r, ajeno, "Juan", AHORA));
    }

    @Test
    void rechazaUnPorcentajeDeAnticipoFueraDeRango() {
        when(config.anticipoPorcentaje()).thenReturn(150);
        assertThrows(ReglaDominioException.class, () -> servicio.anticipoRequerido(reserva(100_000, false)));
    }
}
