package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** Ciclo de vida de la reserva: RN-08, RN-10 (registro), RN-21 (vencimiento) y no-show. */
class ReservaCicloDeVidaTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 11, 2, 10, 0);

    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private Reserva pendiente() {
        return Reserva.crear(new CodigoReserva("RES-2026-00001"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), CREADA);
    }

    private Reserva confirmada() {
        Reserva r = pendiente();
        r.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Juan", CREADA);
        r.confirmar("Juan", CREADA);
        return r;
    }

    // ---------- RN-10: registro ----------

    @Test
    void elGrupoSeRegistraElDiaDeEntradaDeUnaReservaConfirmadaRN10() {
        Reserva r = confirmada();
        r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));
        assertEquals(EstadoReserva.EN_CURSO, r.getEstado());
    }

    @Test
    void noSePermiteElRegistroAntesDeLaFechaDeEntradaRN10() {
        Reserva r = confirmada();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 9, 23, 59)));

        assertEquals("No se puede registrar la llegada antes de la fecha de entrada", ex.getMessage());
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
    }

    @Test
    void noSePermiteElRegistroSobreUnaReservaQueNoEstaConfirmadaRN10() {
        Reserva r = pendiente();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0)));

        assertEquals("No se puede pasar de PENDIENTE a EN_CURSO", ex.getMessage());
    }

    // ---------- RN-08: transiciones ----------

    @Test
    void laSalidaSoloEsPosibleDesdeEnCursoRN08() {
        Reserva r = confirmada();
        assertThrows(ReglaDominioException.class, () -> r.registrarSalida("Recepción", CREADA));

        r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));
        r.registrarSalida("Recepción", LocalDateTime.of(2026, 12, 12, 10, 0));
        assertEquals(EstadoReserva.FINALIZADA, r.getEstado());
    }

    @Test
    void unaReservaFinalizadaNoAdmiteNingunaOperacionRN08() {
        Reserva r = confirmada();
        r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));
        r.registrarSalida("Recepción", LocalDateTime.of(2026, 12, 12, 10, 0));

        assertThrows(ReglaDominioException.class, () -> r.cancelar("x", "Juan", CREADA));
        assertThrows(ReglaDominioException.class, () -> r.confirmar("Juan", CREADA));
        assertThrows(ReglaDominioException.class, () -> r.registrarLlegada("Recepción", CREADA));
    }

    @Test
    void cancelarExigeMotivoYRegistraLaVersionDeLaPoliticaAplicada() {
        Reserva r = pendiente();
        assertThrows(ReglaDominioException.class, () -> r.cancelar(" ", "Juan", CREADA));

        r.cancelar("Cambio de planes", "Juan", CREADA);

        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
        assertTrue(r.getHistorial().get(r.getHistorial().size() - 1).observacion().contains("versión 1"));
    }

    // ---------- RN-21: vencimiento ----------

    @Test
    void unaReservaPendienteQueSuperaElPlazoDeConfirmacionSeCancelaRN21() {
        Reserva r = pendiente();

        r.vencer(new PlazoConfirmacion(24), CREADA.plusHours(25));

        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
        assertEquals("VENCIMIENTO", r.getHistorial().get(r.getHistorial().size() - 1).accion());
    }

    @Test
    void dentroDelPlazoDeConfirmacionNoVenceRN21() {
        Reserva r = pendiente();

        assertThrows(ReglaDominioException.class, () -> r.vencer(new PlazoConfirmacion(24), CREADA.plusHours(24)));
        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
    }

    @Test
    void soloVenceUnaReservaPendienteRN21() {
        Reserva r = confirmada();
        assertThrows(ReglaDominioException.class, () -> r.vencer(new PlazoConfirmacion(24), CREADA.plusDays(5)));
    }

    // ---------- No-show ----------

    @Test
    void elNoShowSoloSeDeclaraVencidaLaHoraLimiteDelDiaDeEntrada() {
        Reserva r = confirmada();
        LocalTime limite = LocalTime.of(22, 0);

        assertThrows(ReglaDominioException.class,
                () -> r.declararNoShow(limite, "Sistema", LocalDateTime.of(2026, 12, 10, 21, 59)));
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());

        r.declararNoShow(limite, "Sistema", LocalDateTime.of(2026, 12, 10, 22, 0));
        assertEquals(EstadoReserva.NO_SHOW, r.getEstado());
    }

    @Test
    void elNoShowSoloAplicaAReservasConfirmadasRN08() {
        Reserva r = pendiente();
        assertThrows(ReglaDominioException.class,
                () -> r.declararNoShow(LocalTime.of(22, 0), "Sistema", LocalDateTime.of(2026, 12, 11, 0, 0)));
    }
}
