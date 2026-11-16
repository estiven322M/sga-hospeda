package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.exception.MensajeRepetidoException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RecepcionReservaExternaService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 7.8 y 10.3.1: la credencial (solo su hash), la bitácora, la revisión de un conflicto y el mensaje repetido. */
class CanalExternoValoresTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    // ---------- credencial ----------

    @Test
    void laCredencialGeneradaEsAleatoriaDeAltaEntropiaYLlevaSuPrefijo() {
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            CredencialCanal credencial = CredencialCanal.generar();
            assertTrue(credencial.valor().startsWith(CredencialCanal.PREFIJO));
            assertTrue(credencial.valor().length() >= 40, "256 bits en base 64: " + credencial.valor().length());
            assertTrue(vistas.add(credencial.valor()), "nunca se repite");
        }
    }

    @Test
    void elHashEsDeterministaDe64CaracteresYNoContieneLaCredencial() {
        CredencialCanal credencial = CredencialCanal.generar();

        assertEquals(credencial.hash(), CredencialCanal.hashDe(credencial.valor()));
        assertEquals(credencial.hash(), CredencialCanal.hashDe("  " + credencial.valor() + " "), "los espacios de los extremos no cuentan");
        assertEquals(64, credencial.hash().length());
        assertFalse(credencial.hash().contains(credencial.valor()));
        assertNotEquals(credencial.hash(), CredencialCanal.generar().hash());
    }

    @Test
    void elHashEsSha256DeUnTextoConocido() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", CredencialCanal.hashDe("abc"));
    }

    @Test
    void laCredencialNuncaSeImprimeEnUnaTraza() {
        CredencialCanal credencial = CredencialCanal.generar();

        assertFalse(credencial.toString().contains(credencial.valor()));
        assertFalse(credencial.toString().contains(credencial.hash()));
    }

    @Test
    void unaCredencialVaciaSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> CredencialCanal.hashDe(" "));
        assertThrows(ReglaDominioException.class, () -> CredencialCanal.hashDe(null));
        assertThrows(ReglaDominioException.class, () -> new CredencialCanal(null, "h"));
    }

    // ---------- bitácora ----------

    @Test
    void unEventoRegistraCuandoCanalOperacionIdentificadorResultadoYCodigoHttp() {
        EventoCanal evento = new EventoCanal(AHORA, "DEMO", OperacionCanal.CREAR_RESERVA, "RES-1", ResultadoCanal.ACEPTADA, 201, "ok");

        assertEquals("DEMO", evento.canal());
        assertEquals(201, evento.codigoHttp());
        assertEquals(ResultadoCanal.ACEPTADA, evento.resultado());
    }

    @Test
    void unEventoExigeSusDatosYUnCodigoHttpValido() {
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(null, "DEMO", OperacionCanal.CREAR_RESERVA, null, ResultadoCanal.ACEPTADA, 200, null));
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(AHORA, " ", OperacionCanal.CREAR_RESERVA, null, ResultadoCanal.ACEPTADA, 200, null));
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(AHORA, "DEMO", null, null, ResultadoCanal.ACEPTADA, 200, null));
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(AHORA, "DEMO", OperacionCanal.CREAR_RESERVA, null, null, 200, null));
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(AHORA, "DEMO", OperacionCanal.CREAR_RESERVA, null, ResultadoCanal.ACEPTADA, 99, null));
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(AHORA, "DEMO", OperacionCanal.CREAR_RESERVA, null, ResultadoCanal.ACEPTADA, 600, null));
    }

    @Test
    void unDetalleLargoSeRecortaSinRomperElEvento() {
        EventoCanal evento = new EventoCanal(AHORA, "DEMO", OperacionCanal.CREAR_RESERVA, null, ResultadoCanal.RECHAZADA, 409, "x".repeat(1000));

        assertEquals(EventoCanal.MAXIMO_DETALLE, evento.detalle().length());
        assertTrue(evento.detalle().endsWith("…"));
    }

    // ---------- revisión de un conflicto ----------

    private static RechazoReservaExterna rechazo() {
        return new RechazoReservaExterna(CanalOrigen.EXTERNO, new IdentificadorExterno("DEMO:RES-1"), new IdentificacionApartamento("APT-202"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), "Solapa", AHORA.minusDays(1));
    }

    @Test
    void unConflictoNaceSinRevisar() {
        RechazoReservaExterna rechazo = rechazo();

        assertFalse(rechazo.estaRevisado());
        assertEquals(null, rechazo.getRevision());
    }

    @Test
    void resolverLoMarcaRevisadoConLaNotaQuienYCuando() {
        RechazoReservaExterna rechazo = rechazo();

        rechazo.resolver("  Se contactó al canal: reservará otras fechas ", "Administrador SGA", AHORA);

        assertTrue(rechazo.estaRevisado());
        assertEquals("Se contactó al canal: reservará otras fechas", rechazo.getRevision().nota());
        assertEquals("Administrador SGA", rechazo.getRevision().revisadoPor());
        assertEquals(AHORA, rechazo.getRevision().revisadaEn());
    }

    @Test
    void resolverNoCambiaNingunDatoDelRechazo() {
        RechazoReservaExterna rechazo = rechazo();

        rechazo.resolver("nota", "Admin", AHORA);

        assertEquals("DEMO:RES-1", rechazo.getIdentificadorExterno().valor());
        assertEquals("APT-202", rechazo.getApartamento().valor());
        assertEquals("Solapa", rechazo.getMotivo());
        assertEquals(AHORA.minusDays(1), rechazo.getRechazadaEn());
    }

    @Test
    void laNotaEsObligatoriaYAcotada() {
        assertThrows(ReglaDominioException.class, () -> rechazo().resolver(null, "Admin", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo().resolver("   ", "Admin", AHORA));
        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> rechazo().resolver("n".repeat(501), "Admin", AHORA));
        assertEquals("La nota no puede superar 500 caracteres", e.getMessage());
        assertThrows(ReglaDominioException.class, () -> rechazo().resolver("nota", " ", AHORA));
        assertThrows(ReglaDominioException.class, () -> rechazo().resolver("nota", "Admin", null));
    }

    @Test
    void unaNotaInvalidaNoDejaElConflictoRevisado() {
        RechazoReservaExterna rechazo = rechazo();

        assertThrows(ReglaDominioException.class, () -> rechazo.resolver(" ", "Admin", AHORA));

        assertFalse(rechazo.estaRevisado());
    }

    @Test
    void unConflictoSeRevisaUnaSolaVezYLaSegundaNoPisaLaPrimera() {
        RechazoReservaExterna rechazo = rechazo();
        rechazo.resolver("primera nota", "Ana", AHORA);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> rechazo.resolver("segunda nota", "Beto", AHORA.plusHours(1)));

        assertEquals("El conflicto ya fue revisado por Ana", e.getMessage());
        assertEquals("primera nota", rechazo.getRevision().nota());
    }

    @Test
    void reconstruirRestauraLaIdentidadYLaRevision() {
        RevisionRechazo revision = new RevisionRechazo("nota", "Ana", AHORA);
        RechazoReservaExterna restaurado = RechazoReservaExterna.reconstruir(5L, CanalOrigen.EXTERNO, new IdentificadorExterno("DEMO:RES-1"),
                new IdentificacionApartamento("APT-202"), new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), "Solapa", AHORA, revision);

        assertEquals(5L, restaurado.getId());
        assertTrue(restaurado.estaRevisado());
    }

    // ---------- RN-19: mensaje repetido ----------

    @Test
    void elMensajeRepetidoEsUnaReglaDelNegocioEspecifica() {
        ReservaRepository reservas = mock(ReservaRepository.class);
        var existente = mock(co.edu.uniquindio.sga.domain.entity.Reserva.class);
        when(existente.getCodigo()).thenReturn(new CodigoReserva("RES-2027-00001"));
        IdentificadorExterno id = new IdentificadorExterno("DEMO:RES-1");
        when(reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, id)).thenReturn(java.util.Optional.of(existente));

        MensajeRepetidoException e = assertThrows(MensajeRepetidoException.class,
                () -> new RecepcionReservaExternaService(reservas).verificarRecepcion(CanalOrigen.EXTERNO, id));

        assertTrue(e instanceof ReglaDominioException, "sigue siendo 409 como antes");
        assertTrue(e.getMessage().contains("RES-2027-00001"));
    }
}
