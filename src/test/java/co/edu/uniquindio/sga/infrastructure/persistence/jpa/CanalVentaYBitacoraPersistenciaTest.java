package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.EstanciaPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.RechazoReservaExternaPersistenceMapper;

/** Persistir no cambia el dominio: los canales (con el hash de su credencial), la bitácora con sus filtros y la revisión de un conflicto. */
@DataJpaTest
@Import({CanalVentaJpaRepository.class, BitacoraCanalJpaRepository.class, RechazoReservaExternaJpaRepository.class,
        RechazoReservaExternaPersistenceMapper.class, EstanciaPersistenceMapper.class})
class CanalVentaYBitacoraPersistenciaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Autowired private CanalVentaJpaRepository canales;
    @Autowired private BitacoraCanalJpaRepository bitacora;
    @Autowired private RechazoReservaExternaJpaRepository rechazos;

    private static CanalVenta canal(String codigo, String credencial) {
        return new CanalVenta(codigo, "Canal " + codigo, TipoCanalVenta.AGENCIA_DE_VIAJES, CredencialCanal.de(credencial).hash());
    }

    private static EventoCanal evento(String canal, LocalDateTime cuando, ResultadoCanal resultado, int http) {
        return new EventoCanal(cuando, canal, OperacionCanal.CREAR_RESERVA, "RES-1", resultado, http, "detalle");
    }

    // ---------- canales ----------

    @Test
    void guardaYRecuperaElCanalConElHashDeSuCredencialYSuEstado() {
        canales.guardar(canal("DEMO", "sgc_uno"));

        CanalVenta leido = canales.buscarPorCodigo("demo").orElseThrow();

        assertEquals("DEMO", leido.getCodigo());
        assertEquals(TipoCanalVenta.AGENCIA_DE_VIAJES, leido.getTipo());
        assertEquals(CredencialCanal.hashDe("sgc_uno"), leido.getCredencialHash());
        assertTrue(leido.isActivo());
    }

    @Test
    void seEncuentraAlCanalPorElHashDeSuCredencial() {
        canales.guardar(canal("UNO", "sgc_uno"));
        canales.guardar(canal("DOS", "sgc_dos"));

        assertEquals("DOS", canales.buscarPorCredencialHash(CredencialCanal.hashDe("sgc_dos")).orElseThrow().getCodigo());
        assertTrue(canales.buscarPorCredencialHash(CredencialCanal.hashDe("sgc_otra")).isEmpty());
    }

    @Test
    void desactivarSeGuardaSinDuplicarNiBorrar() {
        CanalVenta guardado = canales.guardar(canal("DEMO", "sgc_uno"));
        guardado.desactivar();
        canales.guardar(guardado);

        CanalVenta leido = canales.buscarPorCodigo("DEMO").orElseThrow();

        assertFalse(leido.isActivo());
        assertEquals(1, canales.listar(SolicitudPagina.primera()).totalElementos());
        // un canal desactivado sigue encontrándose por su credencial: así se distingue "desconocida" de "desactivada"
        assertTrue(canales.buscarPorCredencialHash(CredencialCanal.hashDe("sgc_uno")).isPresent());
    }

    @Test
    void elListadoVaDeDiezEnDiezPorCodigo() {
        for (int i = 0; i < 12; i++) {
            canales.guardar(canal(String.format("C%02d", i), "sgc_" + i));
        }

        Pagina<CanalVenta> primera = canales.listar(SolicitudPagina.primera());
        Pagina<CanalVenta> segunda = canales.listar(new SolicitudPagina(1));

        assertEquals(12, primera.totalElementos());
        assertEquals(10, primera.contenido().size());
        assertEquals("C00", primera.contenido().get(0).getCodigo());
        assertEquals(2, segunda.contenido().size());
    }

    // ---------- bitácora ----------
    // El registro usa su propia transacción (REQUIRES_NEW) y por eso NO se revierte al terminar cada prueba: cada una usa un canal
    // propio y filtra por él, así no depende de lo que dejaron las demás.

    @Test
    void laBitacoraListaDelMasRecienteAlMasAntiguoDeDiezEnDiez() {
        for (int i = 0; i < 12; i++) {
            bitacora.registrar(evento("LISTA", AHORA.plusMinutes(i), ResultadoCanal.ACEPTADA, 201));
        }

        Pagina<EventoCanal> primera = bitacora.listar("LISTA", null, null, SolicitudPagina.primera());

        assertEquals(12, primera.totalElementos());
        assertEquals(10, primera.contenido().size());
        assertEquals(AHORA.plusMinutes(11), primera.contenido().get(0).registradoEn());
        assertEquals(2, bitacora.listar("LISTA", null, null, new SolicitudPagina(1)).contenido().size());
    }

    @Test
    void laBitacoraFiltraPorCanalSinDistinguirMayusculas() {
        bitacora.registrar(evento("FILTRO1", AHORA, ResultadoCanal.ACEPTADA, 201));
        bitacora.registrar(evento("FILTRO2", AHORA, ResultadoCanal.RECHAZADA, 409));
        bitacora.registrar(evento("FILTRO1", AHORA.plusMinutes(1), ResultadoCanal.REPETIDA, 409));

        Pagina<EventoCanal> deUno = bitacora.listar("filtro1", null, null, SolicitudPagina.primera());

        assertEquals(2, deUno.totalElementos());
        assertTrue(deUno.contenido().stream().allMatch(e -> e.canal().equals("FILTRO1")));
        assertTrue(bitacora.listar(" ", null, null, SolicitudPagina.primera()).totalElementos() >= 3, "un canal en blanco no filtra");
    }

    @Test
    void laBitacoraFiltraPorRangoDeFechasInclusivoEnAmbosExtremos() {
        bitacora.registrar(evento("FECHAS", LocalDateTime.of(2026, 11, 10, 23, 59), ResultadoCanal.ACEPTADA, 201));
        bitacora.registrar(evento("FECHAS", LocalDateTime.of(2026, 11, 11, 0, 0), ResultadoCanal.ACEPTADA, 201));
        bitacora.registrar(evento("FECHAS", LocalDateTime.of(2026, 11, 12, 23, 59), ResultadoCanal.ACEPTADA, 201));
        bitacora.registrar(evento("FECHAS", LocalDateTime.of(2026, 11, 13, 0, 0), ResultadoCanal.ACEPTADA, 201));

        assertEquals(2, bitacora.listar("FECHAS", LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12), SolicitudPagina.primera()).totalElementos());
        assertEquals(3, bitacora.listar("FECHAS", LocalDate.of(2026, 11, 11), null, SolicitudPagina.primera()).totalElementos());
        assertEquals(2, bitacora.listar("FECHAS", null, LocalDate.of(2026, 11, 11), SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void elEventoViajaCompletoYVuelveIgual() {
        EventoCanal original = new EventoCanal(AHORA, "VUELTA", OperacionCanal.CANCELAR_RESERVA, "RES-7", ResultadoCanal.NO_ENCONTRADA, 404, "No existe");
        bitacora.registrar(original);

        assertEquals(original, bitacora.listar("VUELTA", null, null, SolicitudPagina.primera()).contenido().get(0));
    }

    // ---------- conflictos ----------

    private RechazoReservaExterna rechazoGuardado() {
        return rechazos.guardar(new RechazoReservaExterna(CanalOrigen.EXTERNO, new IdentificadorExterno("DEMO:RES-1"),
                new IdentificacionApartamento("APT-202"), new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), "Solapa", AHORA));
    }

    @Test
    void unConflictoGuardadoRecibeIdentidadYNaceSinRevisar() {
        RechazoReservaExterna guardado = rechazoGuardado();

        assertNotNull(guardado.getId());
        assertFalse(guardado.estaRevisado());
        assertNull(guardado.getRevision());
    }

    @Test
    void laRevisionSeGuardaSinCambiarLosDemasDatosNiDuplicarElConflicto() {
        RechazoReservaExterna guardado = rechazoGuardado();
        guardado.resolver("Se habló con el canal", "Administrador SGA", AHORA.plusHours(2));
        rechazos.guardar(guardado);

        RechazoReservaExterna leido = rechazos.buscarPorId(guardado.getId()).orElseThrow();

        assertTrue(leido.estaRevisado());
        assertEquals("Se habló con el canal", leido.getRevision().nota());
        assertEquals("Administrador SGA", leido.getRevision().revisadoPor());
        assertEquals(AHORA.plusHours(2), leido.getRevision().revisadaEn());
        assertEquals("Solapa", leido.getMotivo());
        assertEquals("DEMO:RES-1", leido.getIdentificadorExterno().valor());
        assertEquals(1, rechazos.listar(SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void elListadoFiltraPorRevisionYBuscarUnIdInexistenteEsVacio() {
        RechazoReservaExterna revisado = rechazoGuardado();
        revisado.resolver("ok", "Admin", AHORA);
        rechazos.guardar(revisado);
        rechazoGuardado();

        assertEquals(2, rechazos.listar(null, SolicitudPagina.primera()).totalElementos());
        assertEquals(1, rechazos.listar(true, SolicitudPagina.primera()).totalElementos());
        assertEquals(1, rechazos.listar(false, SolicitudPagina.primera()).totalElementos());
        assertTrue(rechazos.listar(false, SolicitudPagina.primera()).contenido().stream().noneMatch(RechazoReservaExterna::estaRevisado));
        assertTrue(rechazos.buscarPorId(99_999).isEmpty());
    }
}
