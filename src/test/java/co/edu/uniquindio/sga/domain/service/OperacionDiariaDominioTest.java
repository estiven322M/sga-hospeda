package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** Operación diaria: salida con folio cerrado (7.6), no-show con la política congelada (7.4) y estado operativo. */
class OperacionDiariaDominioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private final RegistroSalidaService salida = new RegistroSalidaService();

    private Reserva reserva(int version) {
        return Reserva.crear(codigo, new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(version), AHORA);
    }

    private Folio folio() {
        return new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
    }

    private PoliticaCancelacion politica(int version) {
        return new PoliticaCancelacion(new VersionPolitica(version), LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 80)));
    }

    // ---------- Salida exige folio cerrado (7.6) ----------

    @Test
    void laSalidaRequiereElFolioCerrado() {
        Folio abierto = folio();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> salida.verificarQueElFolioEstaCerrado(reserva(1), abierto));

        assertTrue(ex.getMessage().contains("el folio debe cerrarse primero"));
    }

    @Test
    void conElFolioCerradoLaSalidaPuedeContinuar() {
        Folio f = folio();
        f.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(200_000)));
        f.cerrar(null); // saldo cero: no requiere autorización

        assertDoesNotThrow(() -> salida.verificarQueElFolioEstaCerrado(reserva(1), f));
    }

    @Test
    void conSaldoPendienteElFolioSoloCierraConAutorizacionRN17() {
        Folio f = folio();
        assertThrows(ReglaDominioException.class, () -> f.cerrar(null));
        assertThrows(ReglaDominioException.class, () -> f.cerrar("  "));

        f.cerrar("Administración: huésped pagará por transferencia");

        assertDoesNotThrow(() -> salida.verificarQueElFolioEstaCerrado(reserva(1), f));
        assertEquals("Administración: huésped pagará por transferencia", f.getAutorizacionCierre());
    }

    @Test
    void rechazaUnFolioQueNoEsDeLaReserva() {
        Folio ajeno = new Folio(new CodigoReserva("RES-2026-00099"), new Cargo("Alojamiento", Dinero.de(1)));
        ajeno.cerrar("ok");
        assertThrows(ReglaDominioException.class, () -> salida.verificarQueElFolioEstaCerrado(reserva(1), ajeno));
    }

    // ---------- No-show: consecuencia de la política congelada ----------

    @Test
    void elNoShowRetieneElTramoDeAntelacionCeroDeLaPoliticaCongelada() {
        var servicio = new RetencionCancelacionService(mock(ConfiguracionAlojamiento.class));

        assertEquals(Dinero.de(160_000), servicio.retencionPorNoShow(reserva(2), politica(2))); // 80 % de 200.000
    }

    @Test
    void elNoShowRechazaUnaPoliticaDistintaALaCongeladaRN13() {
        var servicio = new RetencionCancelacionService(mock(ConfiguracionAlojamiento.class));

        assertThrows(ReglaDominioException.class, () -> servicio.retencionPorNoShow(reserva(2), politica(3)));
    }

    @Test
    void liquidarNoShowRevierteElAlojamientoYRegistraLaPenalidadComoNoShow() {
        Folio f = folio();
        f.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(100_000)));

        f.liquidarNoShow(Dinero.de(160_000));

        assertEquals(Dinero.de(60_000), f.saldo()); // 160.000 retenidos - 100.000 pagados
        assertEquals("Penalidad por no-show", f.obtenerCargos().get(1).concepto());
        assertEquals("Reverso del alojamiento por no-show", f.obtenerAjustes().get(0).concepto());
    }

    @Test
    void laRetencionNoShowNoPuedeSuperarLoCargadoNiHacerseSobreUnFolioCerrado() {
        Folio f = folio();
        assertThrows(ReglaDominioException.class, () -> f.liquidarNoShow(Dinero.de(200_001)));

        f.cerrar("ok");
        assertThrows(ReglaDominioException.class, () -> f.liquidarNoShow(Dinero.CERO));
    }

    // ---------- Estado operativo ----------

    private Apartamento apartamento() {
        Apartamento apt = new Apartamento(new IdentificacionApartamento("APT-101"), "Vista", 2, 4);
        apt.activar();
        return apt;
    }

    @Test
    void cicloDeServicioTrasLaSalida() {
        Apartamento apt = apartamento();
        apt.marcarOcupado();
        apt.liberar();
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apt.getEstadoOperativo());
        apt.iniciarPreparacion();
        assertEquals(EstadoOperativo.EN_PREPARACION, apt.getEstadoOperativo());
        apt.marcarPreparado();
        assertEquals(EstadoOperativo.PREPARADO, apt.getEstadoOperativo());
    }

    @Test
    void fueraDeServicioSoloDesdeUnEstadoNoOcupadoYSeReincorporaAPendientePreparacion() {
        Apartamento apt = apartamento();
        apt.declararFueraDeServicio();
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, apt.getEstadoOperativo());

        apt.reincorporarAlServicio();
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apt.getEstadoOperativo());

        Apartamento ocupado = apartamento();
        ocupado.marcarOcupado();
        assertThrows(ReglaDominioException.class, ocupado::declararFueraDeServicio);
    }

    @Test
    void noSeReincorporaUnApartamentoQueNoEstaFueraDeServicio() {
        Apartamento apt = apartamento();
        assertThrows(ReglaDominioException.class, apt::reincorporarAlServicio);
        assertEquals(EstadoOperativo.PREPARADO, apt.getEstadoOperativo());
    }

    @Test
    void soloUnApartamentoPreparadoRecibeUnGrupoRN11() {
        Apartamento enPreparacion = apartamento();
        enPreparacion.marcarOcupado();
        enPreparacion.liberar();
        assertThrows(ReglaDominioException.class, enPreparacion::marcarOcupado);

        Apartamento fuera = apartamento();
        fuera.declararFueraDeServicio();
        assertThrows(ReglaDominioException.class, fuera::marcarOcupado);
    }
}
