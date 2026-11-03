package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-18 (constancia del rechazo) y RN-19 (identificador del canal en la reserva). */
class ReservaExternaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);

    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Estancia estancia = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11));
    private final ValorCongelado valor = new ValorCongelado(List.of(
            new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1)));
    private final IdentificadorExterno mensaje = new IdentificadorExterno("BKG-1");

    private Reserva crear(CanalOrigen canal, IdentificadorExterno identificador) {
        return Reserva.crear(new CodigoReserva("RES-2026-00001"), apt, estancia, titular, List.of(titular), canal,
                identificador, valor, new VersionPolitica(1), AHORA);
    }

    // ---------- IdentificadorExterno ----------

    @Test
    void elIdentificadorSeNormalizaSinEspaciosEnLosExtremos() {
        assertEquals("BKG-1", new IdentificadorExterno("  BKG-1 ").valor());
    }

    @Test
    void elIdentificadorNoPuedeEstarVacioNiSerMuyLargo() {
        assertThrows(ReglaDominioException.class, () -> new IdentificadorExterno(null));
        assertThrows(ReglaDominioException.class, () -> new IdentificadorExterno("   "));
        assertThrows(ReglaDominioException.class,
                () -> new IdentificadorExterno("x".repeat(IdentificadorExterno.LONGITUD_MAXIMA + 1)));
        assertEquals(IdentificadorExterno.LONGITUD_MAXIMA,
                new IdentificadorExterno("x".repeat(IdentificadorExterno.LONGITUD_MAXIMA)).valor().length());
    }

    // ---------- RN-19 en la reserva ----------

    @Test
    void laReservaExternaNaceConSuIdentificadorYLoDejaEnElHistorial() {
        Reserva r = crear(CanalOrigen.EXTERNO, mensaje);

        assertEquals(mensaje, r.getIdentificadorExterno());
        assertTrue(r.getHistorial().get(0).observacion().contains("BKG-1"));
    }

    @Test
    void unaReservaExternaSinIdentificadorSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> crear(CanalOrigen.EXTERNO, null));
    }

    @Test
    void lasReservasDeOtrosCanalesNoLlevanIdentificador() {
        assertNull(crear(CanalOrigen.PORTAL, null).getIdentificadorExterno());
        assertNull(crear(CanalOrigen.DIRECTO, null).getIdentificadorExterno());
        assertThrows(ReglaDominioException.class, () -> crear(CanalOrigen.PORTAL, mensaje));
        assertThrows(ReglaDominioException.class, () -> crear(CanalOrigen.DIRECTO, mensaje));
    }

    @Test
    void reconstruirTambienProtegeLaRelacionEntreCanalEIdentificador() {
        List<EventoReserva> historial = List.of();
        assertThrows(ReglaDominioException.class, () -> Reserva.reconstruir(new CodigoReserva("RES-2026-00001"), apt,
                estancia, titular, List.of(titular), CanalOrigen.EXTERNO, valor, new VersionPolitica(1), AHORA,
                EstadoReserva.PENDIENTE, null, historial, null));
        assertThrows(ReglaDominioException.class, () -> Reserva.reconstruir(new CodigoReserva("RES-2026-00001"), apt,
                estancia, titular, List.of(titular), CanalOrigen.PORTAL, valor, new VersionPolitica(1), AHORA,
                EstadoReserva.PENDIENTE, null, historial, mensaje));
        Reserva restaurada = Reserva.reconstruir(new CodigoReserva("RES-2026-00001"), apt, estancia, titular,
                List.of(titular), CanalOrigen.EXTERNO, valor, new VersionPolitica(1), AHORA,
                EstadoReserva.PENDIENTE, null, historial, mensaje);
        assertEquals(mensaje, restaurada.getIdentificadorExterno());
    }

    @Test
    void modificarLaReservaNoCambiaSuIdentificadorExterno() {
        Reserva r = crear(CanalOrigen.EXTERNO, mensaje);

        r.modificarEstancia(estancia, valor, List.of(titular), "Juan", AHORA);

        assertEquals(mensaje, r.getIdentificadorExterno());
    }

    // ---------- RN-18: constancia del rechazo ----------

    private RechazoReservaExterna rechazo(CanalOrigen canal, IdentificadorExterno id, IdentificacionApartamento a,
                                          Estancia e, String motivo, LocalDateTime cuando) {
        return new RechazoReservaExterna(canal, id, a, e, motivo, cuando);
    }

    @Test
    void elRechazoGuardaLoQuePidioElCanalYLaRazon() {
        RechazoReservaExterna r = rechazo(CanalOrigen.EXTERNO, mensaje, apt, estancia, "Solapa una reserva", AHORA);

        assertEquals(CanalOrigen.EXTERNO, r.getCanalOrigen());
        assertEquals(mensaje, r.getIdentificadorExterno());
        assertEquals(apt, r.getApartamento());
        assertEquals(estancia, r.getEstancia());
        assertEquals("Solapa una reserva", r.getMotivo());
        assertEquals(AHORA, r.getRechazadaEn());
    }

    @Test
    void elRechazoSoloAplicaACanalesExternosYExigeTodosSusDatos() {
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.PORTAL, mensaje, apt, estancia, "x", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(null, mensaje, apt, estancia, "x", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.EXTERNO, null, apt, estancia, "x", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.EXTERNO, mensaje, null, estancia, "x", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.EXTERNO, mensaje, apt, null, "x", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.EXTERNO, mensaje, apt, estancia, " ", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.EXTERNO, mensaje, apt, estancia, null, AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo(CanalOrigen.EXTERNO, mensaje, apt, estancia, "x", null));
    }
}
