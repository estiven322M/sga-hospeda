package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-21: una reserva PENDIENTE que supera el plazo de confirmación pasa a CANCELADA sin penalidad. */
class VencimientoReservaDominioTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final PlazoConfirmacion plazo = new PlazoConfirmacion(24);
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private Reserva pendiente() {
        return Reserva.crear(codigo, new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), CREADA);
    }

    // ---------- límite de creación ----------

    @Test
    void elLimiteDeCreacionEsElInstanteMenosElPlazo() {
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 0), plazo.limiteDeCreacion(LocalDateTime.of(2026, 11, 3, 12, 0)));
    }

    @Test
    void unaReservaCreadaAntesDelLimiteVenceYUnaCreadaEnElLimiteNoRN21() {
        LocalDateTime ahora = CREADA.plusHours(30);
        LocalDateTime limite = plazo.limiteDeCreacion(ahora); // 2-nov 10:00 + 6 h

        assertTrue(plazo.venceAntesDe(CREADA, ahora));
        assertTrue(CREADA.isBefore(limite)); // consistente con el criterio del puerto
        assertFalse(plazo.venceAntesDe(limite, ahora)); // creada justo en el límite: aún no vence
    }

    // ---------- Reserva.haVencido ----------

    @Test
    void haVencidoSoloSiEstaPendienteYSuperoElPlazo() {
        Reserva r = pendiente();

        assertFalse(r.haVencido(plazo, CREADA.plusHours(24))); // exactamente el plazo: todavía no
        assertTrue(r.haVencido(plazo, CREADA.plusHours(24).plusMinutes(1)));
    }

    @Test
    void unaReservaQueNoEstaPendienteNuncaHaVencido() {
        Reserva confirmada = pendiente();
        confirmada.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Juan", CREADA);
        confirmada.confirmar("Juan", CREADA);
        assertFalse(confirmada.haVencido(plazo, CREADA.plusDays(30)));

        Reserva cancelada = pendiente();
        cancelada.cancelar("Cambio de planes", "Juan", CREADA);
        assertFalse(cancelada.haVencido(plazo, CREADA.plusDays(30)));
    }

    @Test
    void haVencidoNoModificaLaReserva() {
        Reserva r = pendiente();
        r.haVencido(plazo, CREADA.plusDays(5));
        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
        assertEquals(1, r.getHistorial().size());
    }

    @Test
    void venceLaReservaPendienteYQuedaRegistradoEnElHistorial() {
        Reserva r = pendiente();

        r.vencer(plazo, CREADA.plusHours(25));

        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
        assertFalse(r.getEstado().retieneDisponibilidad()); // libera las noches (RN-12)
        assertEquals("VENCIMIENTO", r.getHistorial().get(1).accion());
        assertEquals("SISTEMA", r.getHistorial().get(1).autor());
    }

    @Test
    void noVenceUnaReservaDentroDelPlazoNiDosVeces() {
        Reserva r = pendiente();
        assertThrows(ReglaDominioException.class, () -> r.vencer(plazo, CREADA.plusHours(23)));

        r.vencer(plazo, CREADA.plusHours(25));
        assertThrows(ReglaDominioException.class, () -> r.vencer(plazo, CREADA.plusHours(26)));
    }

    // ---------- Folio.liquidarVencimiento ----------

    private Folio folio() {
        return new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
    }

    @Test
    void elVencimientoSinPagosDejaElFolioEnCeroSinPenalidad() {
        Folio f = folio();

        f.liquidarVencimiento();

        assertEquals(Dinero.CERO, f.saldo());
        assertEquals(1, f.obtenerCargos().size()); // no se crea penalidad
        assertEquals("Reverso del alojamiento por vencimiento de la reserva", f.obtenerAjustes().get(0).concepto());
    }

    @Test
    void siHabiaUnPagoParcialElVencimientoLoDejaComoSaldoAFavor() {
        Folio f = folio();
        f.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 2), Dinero.de(50_000)));

        f.liquidarVencimiento();

        assertEquals(Dinero.de(-50_000), f.saldo());
    }

    @Test
    void noSeLiquidaElVencimientoDeUnFolioCerrado() {
        Folio f = folio();
        f.cerrar("Autorizado");
        assertThrows(ReglaDominioException.class, f::liquidarVencimiento);
    }
}
