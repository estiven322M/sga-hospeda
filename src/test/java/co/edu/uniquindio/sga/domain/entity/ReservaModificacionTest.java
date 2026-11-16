package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.testsupport.ReservaDePrueba;

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
                new VersionPolitica(2), AHORA, ReservaDePrueba.EDAD_ADULTO);
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
                valor(LocalDate.of(2026, 12, 10), 3, 100_000, 1), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO);

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
                valor(LocalDate.of(2026, 12, 10), 2, 100_000, 2), new VersionPolitica(2), AHORA, ReservaDePrueba.EDAD_ADULTO);

        Dinero diferencia = r.modificarEstancia(r.getEstancia(), valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1),
                List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO);

        assertEquals(Dinero.de(-200_000), diferencia);
        assertEquals(List.of(titular), r.getOcupantes());
    }

    @Test
    void registraLaModificacionEnElHistorialSinCambiarElEstado() {
        Reserva r = confirmada();
        r.modificarEstancia(r.getEstancia(), r.getValor(), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        assertEquals("MODIFICACION", r.getHistorial().get(r.getHistorial().size() - 1).accion());
    }

    @Test
    void noSePuedeModificarUnaReservaEnCursoRN14() {
        Reserva r = confirmada();
        r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(titular), "Juan", LocalDateTime.of(2026, 12, 10, 17, 0), ReservaDePrueba.EDAD_ADULTO));
    }

    @Test
    void noSePuedeModificarUnaReservaTerminal() {
        Reserva r = reservaDel10al12();
        r.cancelar("Cambio de planes", "Juan", AHORA);

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
    }

    @Test
    void elTitularDebeSeguirEntreLosOcupantes() {
        Reserva r = reservaDel10al12();
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(acompanante), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
    }

    @Test
    void exigeOcupantesSinRepetirYDatosCompletos() {
        Reserva r = reservaDel10al12();
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), r.getValor(),
                List.of(titular, titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(null, r.getValor(),
                List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(r.getEstancia(), null,
                List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
    }

    @Test
    void laNuevaFechaDeEntradaNoPuedeSerPasadaRN04() {
        Reserva r = reservaDel10al12();
        Estancia pasada = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(pasada,
                valor(LocalDate.of(2026, 10, 1), 2, 100_000, 1), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));

        assertEquals("La fecha de entrada no puede ser anterior a la fecha actual", ex.getMessage());
    }

    @Test
    void elDesgloseDebeCorresponderALasNochesDeLaNuevaEstancia() {
        Reserva r = reservaDel10al12();
        Estancia tresNoches = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13));

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(tresNoches,
                valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertEquals(2, r.getEstancia().noches()); // sin efectos parciales
    }

    // --- RN-14: cambio de apartamento ---

    private final IdentificacionApartamento otroApartamento = new IdentificacionApartamento("APT-202");

    @Test
    void permiteCambiarDeApartamentoConElValorDelNuevoYConservaLaPolitica() {
        Reserva r = confirmada();

        Dinero diferencia = r.modificarEstancia(otroApartamento, r.getEstancia(),
                valor(LocalDate.of(2026, 12, 10), 2, 150_000, 1), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO);

        assertEquals(otroApartamento, r.getApartamento());
        assertEquals(Dinero.de(100_000), diferencia);
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        assertEquals(new VersionPolitica(2), r.getPolitica());
        EventoReserva ultimo = r.getHistorial().get(r.getHistorial().size() - 1);
        assertEquals("MODIFICACION", ultimo.accion());
        assertTrue(ultimo.observacion().contains("APT-101 -> APT-202"));
    }

    @Test
    void sinCambiarDeApartamentoElHistorialNoMencionaUnCambio() {
        Reserva r = reservaDel10al12();

        r.modificarEstancia(r.getApartamento(), r.getEstancia(), r.getValor(), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO);

        assertEquals(new IdentificacionApartamento("APT-101"), r.getApartamento());
        assertFalse(r.getHistorial().get(r.getHistorial().size() - 1).observacion().contains("apartamento:"));
    }

    @Test
    void noSePuedeCambiarDeApartamentoSinIndicarElNuevo() {
        Reserva r = reservaDel10al12();

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(null, r.getEstancia(), r.getValor(),
                List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertEquals(new IdentificacionApartamento("APT-101"), r.getApartamento());
    }

    @Test
    void noSePuedeCambiarDeApartamentoUnaReservaEnCursoRN14() {
        Reserva r = confirmada();
        r.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(otroApartamento, r.getEstancia(),
                r.getValor(), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertEquals(new IdentificacionApartamento("APT-101"), r.getApartamento());
    }

    @Test
    void siLaModificacionEsInvalidaElApartamentoNoCambia() {
        Reserva r = reservaDel10al12();
        Estancia pasada = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(otroApartamento, pasada,
                valor(LocalDate.of(2026, 10, 1), 2, 100_000, 1), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));

        assertEquals(new IdentificacionApartamento("APT-101"), r.getApartamento());
    }


    @Test
    void noSePuedeCambiarDeApartamentoUnaReservaTerminada() {
        Reserva r = reservaDel10al12();
        r.cancelar("Cambio de planes", "Juan", AHORA);

        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(otroApartamento, r.getEstancia(),
                r.getValor(), List.of(titular), "Juan", AHORA, ReservaDePrueba.EDAD_ADULTO));
        assertEquals(new IdentificacionApartamento("APT-101"), r.getApartamento());
    }

    // ---------- RP-03 (L-20): adulto responsable al modificar ----------

    private final Ocupante menor = new Ocupante(new DocumentoIdentidad("TI-300"), "Menor",
            LocalDate.of(2011, 6, 1), LocalDate.of(2026, 1, 1)); // 15 años el 10-dic-2026

    private Reserva reservaConTitularMenorYUnAdulto() {
        return Reserva.crear(new CodigoReserva("RES-2026-00003"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), menor,
                List.of(menor, acompanante), CanalOrigen.DIRECTO,
                valor(LocalDate.of(2026, 12, 10), 2, 100_000, 2), new VersionPolitica(2), AHORA,
                ReservaDePrueba.EDAD_ADULTO);
    }

    @Test
    void modificarElGrupoParaQuedarseSinAdultoSeRechazaRP03YNoCambiaNada() {
        Reserva r = reservaConTitularMenorYUnAdulto();
        Dinero valorAntes = r.getValor().total();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> r.modificarEstancia(r.getEstancia(), valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1),
                        List.of(menor), "Recepción", AHORA, ReservaDePrueba.EDAD_ADULTO));

        assertEquals("La reserva debe incluir al menos un ocupante mayor de 18 años", ex.getMessage());
        assertEquals(List.of(menor, acompanante), r.getOcupantes());
        assertEquals(valorAntes, r.getValor().total());
        assertEquals(1, r.getHistorial().size()); // solo la creación: la modificación fallida no deja rastro
    }

    @Test
    void modificarConservandoUnAdultoSePermiteRP03() {
        Reserva r = reservaConTitularMenorYUnAdulto();

        r.modificarEstancia(r.getEstancia(), valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1),
                List.of(menor, acompanante), "Recepción", AHORA, ReservaDePrueba.EDAD_ADULTO);

        assertEquals(List.of(menor, acompanante), r.getOcupantes());
    }

    @Test
    void cambiarLaFechaDeEntradaPuedeQuitarleLaMayoriaDeEdadAlAdultoRP03() {
        // el acompañante nace el 11-dic-2008: el 10-dic-2026 tiene 17 años y el 11-dic cumple 18
        Ocupante casiAdulto = new Ocupante(new DocumentoIdentidad("CC-400"), "Casi adulto",
                LocalDate.of(2008, 12, 11), LocalDate.of(2026, 1, 1));
        Reserva r = Reserva.crear(new CodigoReserva("RES-2026-00004"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22)), casiAdulto,
                List.of(casiAdulto), CanalOrigen.DIRECTO, valor(LocalDate.of(2026, 12, 20), 2, 100_000, 1),
                new VersionPolitica(2), AHORA, ReservaDePrueba.EDAD_ADULTO);

        // adelantar la estancia al 10-dic: cuando llega todavía es menor
        assertThrows(ReglaDominioException.class, () -> r.modificarEstancia(
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                valor(LocalDate.of(2026, 12, 10), 2, 100_000, 1), List.of(casiAdulto), "Recepción", AHORA,
                ReservaDePrueba.EDAD_ADULTO));
        assertEquals(LocalDate.of(2026, 12, 20), r.getEstancia().fechaEntrada());
    }
}
