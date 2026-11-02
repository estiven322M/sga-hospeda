package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** 7.4 Modificación y RN-14: se revalidan las condiciones de creación y la diferencia queda como ajuste. */
class ReservaModificacionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);

    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private final Ocupante acompanante = new Ocupante(new DocumentoIdentidad("CC-200"), "Ana",
            LocalDate.of(1992, 5, 5), LocalDate.of(2026, 1, 1));

    private ValorCongelado valor(LocalDate desde, int noches, long porNoche, int facturables) {
        return new ValorCongelado(desde.datesUntil(desde.plusDays(noches))
                .map(d -> new CargoNoche(d, "ALTA", Dinero.de(porNoche), facturables)).toList());
    }

    private Reserva reservaDel10al12() {
        return Reserva.crear(new CodigoReserva("RES-2026-00001"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1),
                new VersionPolitica(2), AHORA);
    }

    private Reserva confirmada() {
        Reserva r = reservaDel10al12();
        r.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Juan", AHORA);
        r.confirmar("Juan", AHORA);
        return r;
    }

    @Test
    void devuelveLaDiferenciaPositivaAlAmpliarLaEstanciaYConservaLaPolitica() {
        Reserva r = reservaDel10al12();
        Dinero diferencia = r.modificarEstancia(new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13)),
                valor(LocalDate.of(2026, 12, 10), 3, 100_000, 1), List.of(titular), "Juan", AHORA);

        assertEquals(Dinero.de(100_000), diferencia);
        assertEquals(3, r.getEstancia().noches());
        assertEquals(Dinero.de(300_000), r.getValor().total());
        assertEquals(new VersionPolitica(2), r.getPolitica()); // la política no se recalcula
    }

    @Test
    void devuelveLaDiferenciaNegativaAlReducirOcupantesFacturables() {
        Reserva r = Reserva.crear(new CodigoReserva("RES-2026-00002"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular,
                List.of(titular, acompanante), CanalOrigen.DIRECTO,
                valor(LocalDate.of(2026, 12, 10), 2, 100_000, 2), new VersionPolitica(2), AHORA);

        Dinero diferencia = r.modificarEstancia(r.getEstancia(), valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1),
                List.of(titular), "Juan", AHORA);

        assertEquals(Dinero.de(-200_000), diferencia);
        assertEquals(List.of(titular), r.getOcupantes());
    }

    @Test
    void registraLaModificacionEnElHistorialSinCambiarElEstado() {
        Reserva r = confirmada();
        r.modificarEstancia(r.getEstancia(), r.getValor(), List.of(titular), "Juan", AHORA);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        assertEquals("MODIFICACION", r.getHistorial().get(r.getHistorial().size() - 1).accion());
    }

    @Test
    void noSePuedeModificarUnaReservaEnCursoRN14() {
        Reserva r = confirmada();
        r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(titular), "Juan", LocalDateTime.of(2026, 12, 10, 17, 0)));
    }

    @Test
    void noSePuedeModificarUnaReservaTerminal() {
        Reserva r = reservaDel10al12();
        r.cancelar("Cambio de planes", "Juan", AHORA);

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(titular), "Juan", AHORA));
    }

    @Test
    void elTitularDebeSeguirEntreLosOcupantes() {
        Reserva r = reservaDel10al12();
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(acompanante), "Juan", AHORA));
    }

    @Test
    void exigeOcupantesSinRepetirYDatosCompletos() {
        Reserva r = reservaDel10al12();
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(), "Juan", AHORA));
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(titular, titular), "Juan", AHORA));
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(null, r.getValor(),
                List.of(titular), "Juan", AHORA));
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), null,
                List.of(titular), "Juan", AHORA));
    }

    @Test
    void laNuevaFechaDeEntradaNoPuedeSerPasadaRN04() {
        Reserva r = reservaDel10al12();
        Estancia pasada = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(pasada,
                valor(LocalDate.of(2026, 10, 1), 2, 100_000, 1), List.of(titular), "Juan", AHORA));

        assertEquals("La fecha de entrada no puede ser anterior a la fecha actual", ex.getMessage());
    }

    @Test
    void elDesgloseDebeCorresponderALasNochesDeLaNuevaEstancia() {
        Reserva r = reservaDel10al12();
        Estancia tresNoches = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13));

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(tresNoches,
                valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1), List.of(titular), "Juan", AHORA));
        assertEquals(2, r.getEstancia().noches()); // sin efectos parciales
    }
}
