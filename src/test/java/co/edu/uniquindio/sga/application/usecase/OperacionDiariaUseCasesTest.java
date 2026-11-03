package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RegistroSalidaService;
import co.edu.uniquindio.sga.domain.service.RetencionCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** CU-06 llegada, CU-07 salida, CU-10 y estado operativo, CU-11 cierre de folio y no-show. */
@ExtendWith(MockitoExtension.class)
class OperacionDiariaUseCasesTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private PoliticaCancelacionRepository politicaRepository;
    @Mock private ConfiguracionAlojamiento configuracion;

    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final LocalDateTime creada = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final LocalDateTime diaDeEntrada = LocalDateTime.of(2026, 12, 10, 16, 0);
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private Reserva reserva;
    private Folio folio;
    private Apartamento apartamento;

    @BeforeEach
    void setUp() {
        reserva = Reserva.crear(codigo, apt, new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                titular, List.of(titular), CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(2), creada);
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Juan", creada);
        reserva.confirmar("Juan", creada);
        folio = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        apartamento = new Apartamento(apt, "Vista", 2, 4);
        apartamento.activar();
        lenient().when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        lenient().when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.of(folio));
        lenient().when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.of(apartamento));
        lenient().when(reservaRepository.guardar(any(Reserva.class))).then(returnsFirstArg());
        lenient().when(apartamentoRepository.guardar(any(Apartamento.class))).then(returnsFirstArg());
        lenient().when(folioRepository.guardar(any(Folio.class))).then(returnsFirstArg());
    }

    // ---------- CU-06 Registrar llegada ----------

    private RegistrarLlegadaUseCase llegada() {
        return new RegistrarLlegadaUseCase(reservaRepository, apartamentoRepository);
    }

    @Test
    void laLlegadaPasaLaReservaAEnCursoYElApartamentoAOcupadoEnLaMismaTransaccion() {
        Reserva resultado = llegada().ejecutar(codigo, "Recepción", diaDeEntrada);

        assertEquals(EstadoReserva.EN_CURSO, resultado.getEstado());
        assertEquals(EstadoOperativo.OCUPADO, apartamento.getEstadoOperativo());
        verify(reservaRepository).guardar(reserva);
        verify(apartamentoRepository).guardar(apartamento);
    }

    @Test
    void laLlegadaConElApartamentoSinPrepararRN11NoGuardaNinguno() {
        apartamento.marcarOcupado();
        apartamento.liberar(); // PENDIENTE_PREPARACION

        assertThrows(ReglaDominioException.class, () -> llegada().ejecutar(codigo, "Recepción", diaDeEntrada));

        verify(reservaRepository, never()).guardar(any());
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void laLlegadaAntesDeLaFechaDeEntradaRN10NoToqueElApartamento() {
        assertThrows(ReglaDominioException.class,
                () -> llegada().ejecutar(codigo, "Recepción", LocalDateTime.of(2026, 12, 9, 12, 0)));

        assertEquals(EstadoOperativo.PREPARADO, apartamento.getEstadoOperativo());
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void laLlegadaDeUnaReservaNoConfirmadaRN10EsRechazada() {
        reserva.cancelar("Cambio", "Juan", creada);

        assertThrows(ReglaDominioException.class, () -> llegada().ejecutar(codigo, "Recepción", diaDeEntrada));
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void laLlegadaLanzaNoEncontradoSiFaltaLaReservaOElApartamento() {
        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());
        assertThrows(ReservaNoEncontradaException.class, () -> llegada().ejecutar(codigo, "R", diaDeEntrada));

        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class, () -> llegada().ejecutar(codigo, "R", diaDeEntrada));
    }

    // ---------- CU-07 Registrar salida ----------

    private RegistrarSalidaUseCase salida() {
        return new RegistrarSalidaUseCase(reservaRepository, folioRepository, apartamentoRepository,
                new RegistroSalidaService());
    }

    private void conReservaEnCurso() {
        reserva.registrarLlegada("Recepción", diaDeEntrada);
        apartamento.marcarOcupado();
    }

    @Test
    void laSalidaConElFolioCerradoFinalizaLaReservaYDejaElApartamentoPendienteDePreparacion() {
        conReservaEnCurso();
        folio.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 12, 12), Dinero.de(200_000)));
        folio.cerrar(null);

        Reserva resultado = salida().ejecutar(codigo, "Recepción", LocalDateTime.of(2026, 12, 12, 10, 0));

        assertEquals(EstadoReserva.FINALIZADA, resultado.getEstado());
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apartamento.getEstadoOperativo());
        verify(reservaRepository).guardar(reserva);
        verify(apartamentoRepository).guardar(apartamento);
    }

    @Test
    void laSalidaConElFolioAbiertoEsRechazadaYNoCambiaNada() {
        conReservaEnCurso();

        assertThrows(ReglaDominioException.class,
                () -> salida().ejecutar(codigo, "Recepción", LocalDateTime.of(2026, 12, 12, 10, 0)));

        assertEquals(EstadoReserva.EN_CURSO, reserva.getEstado());
        assertEquals(EstadoOperativo.OCUPADO, apartamento.getEstadoOperativo());
        verify(reservaRepository, never()).guardar(any());
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void laSalidaConSaldoPendienteSeCompletaConElCierreAutorizado() {
        conReservaEnCurso();
        folio.cerrar("Autorizado por Admin: pagará después");

        assertEquals(EstadoReserva.FINALIZADA,
                salida().ejecutar(codigo, "Recepción", LocalDateTime.of(2026, 12, 12, 10, 0)).getEstado());
    }

    @Test
    void laSalidaDeUnaReservaQueNoEstaEnCursoEsRechazada() {
        folio.cerrar("Autorizado");

        assertThrows(ReglaDominioException.class,
                () -> salida().ejecutar(codigo, "Recepción", LocalDateTime.of(2026, 12, 12, 10, 0)));
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void laSalidaLanzaNoEncontradoSiFaltaElFolio() {
        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());

        assertThrows(FolioNoEncontradoException.class,
                () -> salida().ejecutar(codigo, "Recepción", LocalDateTime.of(2026, 12, 12, 10, 0)));
    }

    // ---------- CU-11 Cerrar folio ----------

    @Test
    void cierraUnFolioSinSaldoSinNecesitarAutorizacion() {
        folio.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(200_000)));

        Folio resultado = new CerrarFolioUseCase(folioRepository).ejecutar(codigo, null);

        assertTrue(resultado.estaCerrado());
        verify(folioRepository).guardar(folio);
    }

    @Test
    void conSaldoPendienteExigeLaAutorizacionRN17YSiNoSeDaNoSeGuardaNada() {
        var useCase = new CerrarFolioUseCase(folioRepository);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(codigo, null));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(codigo, "  "));

        assertFalse(folio.estaCerrado());
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void conSaldoPendienteYAutorizacionLaRegistraEnElFolioCU11() {
        Folio resultado = new CerrarFolioUseCase(folioRepository)
                .ejecutar(codigo, "Autorizado por Admin: pago por transferencia");

        assertTrue(resultado.estaCerrado());
        assertEquals("Autorizado por Admin: pago por transferencia", resultado.getAutorizacionCierre());
    }

    @Test
    void noSeCierraDosVecesNiUnFolioInexistente() {
        folio.cerrar("Autorizado");
        assertThrows(ReglaDominioException.class, () -> new CerrarFolioUseCase(folioRepository).ejecutar(codigo, "x"));

        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());
        assertThrows(FolioNoEncontradoException.class, () -> new CerrarFolioUseCase(folioRepository).ejecutar(codigo, "x"));
    }

    // ---------- No-show ----------

    private DeclararNoShowUseCase noShow() {
        return new DeclararNoShowUseCase(reservaRepository, folioRepository, politicaRepository,
                new RetencionCancelacionService(configuracion), configuracion);
    }

    private PoliticaCancelacion politica() {
        return new PoliticaCancelacion(new VersionPolitica(2), LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 80)));
    }

    @Test
    void elNoShowAplicaLaPoliticaCongeladaYLiberaLaReserva() {
        when(configuracion.horaLimiteNoShow()).thenReturn(LocalTime.of(22, 0));
        when(politicaRepository.porVersion(new VersionPolitica(2))).thenReturn(Optional.of(politica()));
        folio.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(60_000)));

        Reserva resultado = noShow().ejecutar(codigo, "Sistema", LocalDateTime.of(2026, 12, 10, 22, 30));

        assertEquals(EstadoReserva.NO_SHOW, resultado.getEstado());
        assertFalse(resultado.getEstado().retieneDisponibilidad());
        assertEquals(Dinero.de(100_000), folio.saldo()); // 80 % retenido (160.000) - 60.000 pagados
        verify(folioRepository).guardar(folio);
        verify(reservaRepository).guardar(reserva);
    }

    @Test
    void antesDeLaHoraLimiteNoSePuedeDeclararNoShowYNoSeLiquidaNada() {
        when(configuracion.horaLimiteNoShow()).thenReturn(LocalTime.of(22, 0));
        when(politicaRepository.porVersion(new VersionPolitica(2))).thenReturn(Optional.of(politica()));

        assertThrows(ReglaDominioException.class,
                () -> noShow().ejecutar(codigo, "Sistema", LocalDateTime.of(2026, 12, 10, 21, 0)));

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertEquals(Dinero.de(200_000), folio.saldo());
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void elNoShowLanzaNoEncontradoSiFaltaLaPoliticaCongelada() {
        when(politicaRepository.porVersion(new VersionPolitica(2))).thenReturn(Optional.empty());

        assertThrows(PoliticaCancelacionNoEncontradaException.class,
                () -> noShow().ejecutar(codigo, "Sistema", LocalDateTime.of(2026, 12, 10, 22, 30)));
        verify(reservaRepository, never()).guardar(any());
    }

    // ---------- CU-10 y estado operativo ----------

    @Test
    void elServicioPreparaUnApartamentoPasoAPaso() {
        apartamento.marcarOcupado();
        apartamento.liberar();

        assertEquals(EstadoOperativo.EN_PREPARACION,
                new IniciarPreparacionUseCase(apartamentoRepository).ejecutar(apt).getEstadoOperativo());
        assertEquals(EstadoOperativo.PREPARADO,
                new MarcarPreparadoUseCase(apartamentoRepository).ejecutar(apt).getEstadoOperativo());
        verify(apartamentoRepository, times(2)).guardar(apartamento);
    }

    @Test
    void noSePrepararaUnApartamentoQueNoLoNecesitaNiSeMarcaPreparadoSinIniciar() {
        assertThrows(ReglaDominioException.class, () -> new IniciarPreparacionUseCase(apartamentoRepository).ejecutar(apt));
        assertThrows(ReglaDominioException.class, () -> new MarcarPreparadoUseCase(apartamentoRepository).ejecutar(apt));

        assertEquals(EstadoOperativo.PREPARADO, apartamento.getEstadoOperativo());
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void declaraFueraDeServicioYReincorpora() {
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO,
                new DeclararFueraDeServicioUseCase(apartamentoRepository).ejecutar(apt).getEstadoOperativo());
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION,
                new ReincorporarApartamentoUseCase(apartamentoRepository).ejecutar(apt).getEstadoOperativo());
    }

    @Test
    void noSeDeclaraFueraDeServicioUnApartamentoOcupadoNiSeReincorporaUnoEnServicio() {
        assertThrows(ReglaDominioException.class, () -> new ReincorporarApartamentoUseCase(apartamentoRepository).ejecutar(apt));

        apartamento.marcarOcupado();
        assertThrows(ReglaDominioException.class, () -> new DeclararFueraDeServicioUseCase(apartamentoRepository).ejecutar(apt));
        verify(apartamentoRepository, never()).guardar(any());
    }

    @Test
    void lasOperacionesDeEstadoOperativoLanzanNoEncontradoSiElApartamentoNoExiste() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class, () -> new IniciarPreparacionUseCase(apartamentoRepository).ejecutar(apt));
        assertThrows(ApartamentoNoEncontradoException.class, () -> new MarcarPreparadoUseCase(apartamentoRepository).ejecutar(apt));
        assertThrows(ApartamentoNoEncontradoException.class, () -> new DeclararFueraDeServicioUseCase(apartamentoRepository).ejecutar(apt));
        assertThrows(ApartamentoNoEncontradoException.class, () -> new ReincorporarApartamentoUseCase(apartamentoRepository).ejecutar(apt));
    }
}
