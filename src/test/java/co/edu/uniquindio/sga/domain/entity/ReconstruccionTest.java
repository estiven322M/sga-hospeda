package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** Restauración de entidades desde persistencia: conserva su estado e historial y respeta las invariantes. */
class ReconstruccionTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2025, 3, 1, 9, 0); // histórica: la entrada ya pasó

    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2025, 3, 1));
    private final Ocupante otro = new Ocupante(new DocumentoIdentidad("CC-200"), "Ana",
            LocalDate.of(1992, 1, 1), LocalDate.of(2025, 3, 1));
    private final Estancia estancia = new Estancia(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 12));
    private final ValorCongelado valor = new ValorCongelado(List.of(
            new CargoNoche(LocalDate.of(2025, 3, 10), "BASE", Dinero.de(80_000), 1),
            new CargoNoche(LocalDate.of(2025, 3, 11), "BASE", Dinero.de(80_000), 1)));
    private final List<EventoReserva> historial = List.of(
            new EventoReserva(CREADA, "CREACION", "Juan", null, EstadoReserva.PENDIENTE, "Reserva creada"),
            new EventoReserva(CREADA.plusHours(2), "CONFIRMACION", "Juan", EstadoReserva.PENDIENTE,
                    EstadoReserva.CONFIRMADA, "Reserva confirmada"));

    private Reserva reconstruir(EstadoReserva estado, LocalTime hora, List<Ocupante> ocupantes, ValorCongelado v) {
        return Reserva.reconstruir(new CodigoReserva("RES-2025-00001"), new IdentificacionApartamento("APT-101"),
                estancia, titular, ocupantes, CanalOrigen.DIRECTO, v, new VersionPolitica(1), CREADA, estado, hora,
                historial, null);
    }

    // ---------- Reserva ----------

    @Test
    void restauraUnaReservaHistoricaConSuEstadoFechaDeCreacionEHistorial() {
        Reserva r = reconstruir(EstadoReserva.FINALIZADA, LocalTime.of(16, 0), List.of(titular), valor);

        assertEquals(EstadoReserva.FINALIZADA, r.getEstado()); // y no PENDIENTE
        assertEquals(CREADA, r.getCreadaEn());
        assertEquals(historial, r.getHistorial());
        assertEquals(LocalTime.of(16, 0), r.getHoraEstimadaLlegada());
        assertEquals(Dinero.de(160_000), r.getValor().total());
        assertEquals(new VersionPolitica(1), r.getPolitica());
    }

    @Test
    void laReservaRestauradaSigueSujetaALasReglasDeNegocio() {
        Reserva finalizada = reconstruir(EstadoReserva.FINALIZADA, LocalTime.of(16, 0), List.of(titular), valor);
        assertThrows(ReglaDominioException.class, () -> finalizada.cancelar("x", "Juan", CREADA));

        Reserva confirmada = reconstruir(EstadoReserva.CONFIRMADA, LocalTime.of(16, 0), List.of(titular), valor);
        confirmada.registrarLlegada("Recepción", LocalDateTime.of(2025, 3, 10, 16, 0));
        assertEquals(EstadoReserva.EN_CURSO, confirmada.getEstado());
        assertEquals(3, confirmada.getHistorial().size()); // el historial restaurado + el evento nuevo
    }

    @Test
    void unaReservaCanceladaDesdePendienteNoNecesitaHoraDeLlegada() {
        assertDoesNotThrow(() -> reconstruir(EstadoReserva.CANCELADA, null, List.of(titular), valor));
        assertDoesNotThrow(() -> reconstruir(EstadoReserva.PENDIENTE, null, List.of(titular), valor));
    }

    @Test
    void unaReservaQueEstuvoConfirmadaDebeTenerHoraDeLlegadaRN09() {
        for (EstadoReserva estado : new EstadoReserva[] {EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO,
                EstadoReserva.FINALIZADA, EstadoReserva.NO_SHOW}) {
            assertThrows(ReglaDominioException.class,
                    () -> reconstruir(estado, null, List.of(titular), valor), estado.name());
        }
    }

    @Test
    void rechazaUnTitularFueraDeLosOcupantesOcupantesRepetidosOVaciosYDesgloseIncorrecto() {
        assertThrows(ReglaDominioException.class,
                () -> reconstruir(EstadoReserva.PENDIENTE, null, List.of(otro), valor));
        assertThrows(ReglaDominioException.class,
                () -> reconstruir(EstadoReserva.PENDIENTE, null, List.of(titular, titular), valor));
        assertThrows(ReglaDominioException.class,
                () -> reconstruir(EstadoReserva.PENDIENTE, null, List.of(), valor));
        ValorCongelado unaNoche = new ValorCongelado(List.of(
                new CargoNoche(LocalDate.of(2025, 3, 10), "BASE", Dinero.de(80_000), 1)));
        assertThrows(ReglaDominioException.class,
                () -> reconstruir(EstadoReserva.PENDIENTE, null, List.of(titular), unaNoche));
    }

    @Test
    void rechazaDatosObligatoriosNulos() {
        assertThrows(ReglaDominioException.class, () -> Reserva.reconstruir(null,
                new IdentificacionApartamento("APT-101"), estancia, titular, List.of(titular), CanalOrigen.DIRECTO,
                valor, new VersionPolitica(1), CREADA, EstadoReserva.PENDIENTE, null, historial, null));
        assertThrows(ReglaDominioException.class, () -> Reserva.reconstruir(new CodigoReserva("RES-2025-00001"),
                new IdentificacionApartamento("APT-101"), estancia, titular, List.of(titular), CanalOrigen.DIRECTO,
                valor, new VersionPolitica(1), CREADA, null, null, historial, null));
        assertThrows(ReglaDominioException.class, () -> Reserva.reconstruir(new CodigoReserva("RES-2025-00001"),
                new IdentificacionApartamento("APT-101"), estancia, titular, List.of(titular), CanalOrigen.DIRECTO,
                valor, new VersionPolitica(1), CREADA, EstadoReserva.PENDIENTE, null, null, null));
    }

    @Test
    void elHistorialRestauradoNoSePuedeAlterarDesdeAfuera() {
        List<EventoReserva> origen = new ArrayList<>(historial);
        Reserva r = Reserva.reconstruir(new CodigoReserva("RES-2025-00001"), new IdentificacionApartamento("APT-101"),
                estancia, titular, List.of(titular), CanalOrigen.DIRECTO, valor, new VersionPolitica(1), CREADA,
                EstadoReserva.CONFIRMADA, LocalTime.of(16, 0), origen, null);

        origen.clear(); // modificar la lista de origen no afecta a la reserva
        assertEquals(2, r.getHistorial().size());
        assertThrows(UnsupportedOperationException.class, () -> r.getHistorial().clear());
    }

    // ---------- Apartamento ----------

    @Test
    void restauraUnApartamentoEnSuEstadoOperativoYDeActividad() {
        Apartamento ocupado = Apartamento.reconstruir(new IdentificacionApartamento("apt-101"), "Vista", 2, 4,
                EstadoOperativo.OCUPADO, true);
        assertEquals(EstadoOperativo.OCUPADO, ocupado.getEstadoOperativo());
        assertTrue(ocupado.isActivo());
        assertEquals("APT-101", ocupado.getIdentificacion().valor());

        Apartamento fuera = Apartamento.reconstruir(new IdentificacionApartamento("APT-102"), "Patio", 1, 2,
                EstadoOperativo.FUERA_DE_SERVICIO, false);
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, fuera.getEstadoOperativo());
        assertFalse(fuera.isActivo());
    }

    @Test
    void elApartamentoRestauradoSigueSujetoALasTransicionesDeNegocio() {
        Apartamento ocupado = Apartamento.reconstruir(new IdentificacionApartamento("APT-101"), "Vista", 2, 4,
                EstadoOperativo.OCUPADO, true);

        assertThrows(ReglaDominioException.class, ocupado::declararFueraDeServicio);
        ocupado.liberar();
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, ocupado.getEstadoOperativo());
    }

    @Test
    void elApartamentoRestauradoValidaSusDatos() {
        assertThrows(ReglaDominioException.class, () -> Apartamento.reconstruir(
                new IdentificacionApartamento("APT-101"), " ", 2, 4, EstadoOperativo.PREPARADO, true));
        assertThrows(ReglaDominioException.class, () -> Apartamento.reconstruir(
                new IdentificacionApartamento("APT-101"), "Vista", 0, 4, EstadoOperativo.PREPARADO, true));
        assertThrows(ReglaDominioException.class, () -> Apartamento.reconstruir(
                new IdentificacionApartamento("APT-101"), "Vista", 2, 4, null, true));
    }

    // ---------- Folio: se restaura reproduciendo sus movimientos (no necesita fábrica) ----------

    @Test
    void unFolioCerradoSeRestauraReproduciendoSusMovimientos() {
        Folio f = new Folio(new CodigoReserva("RES-2025-00001"), new Cargo("Alojamiento", Dinero.de(160_000)));
        f.registrarAjuste(new Ajuste("Menos noches", Dinero.de(-20_000)));
        f.registrarPago(new Pago("EFECTIVO", LocalDate.of(2025, 3, 10), Dinero.de(100_000)));
        f.cerrar("Autorizado por Admin: saldo pendiente");

        assertTrue(f.estaCerrado());
        assertEquals(Dinero.de(40_000), f.saldo());
        assertEquals("Autorizado por Admin: saldo pendiente", f.getAutorizacionCierre());
    }
}
