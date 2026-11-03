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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.*;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.service.RecepcionReservaExternaService;
import co.edu.uniquindio.sga.domain.valueobject.*;

@ExtendWith(MockitoExtension.class)
class CrearReservaUseCaseTest {

    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private PoliticaCancelacionRepository politicaRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private GeneradorCodigoReserva generadorCodigo;
    @Mock private DisponibilidadApartamentoService disponibilidad;
    @Mock private CotizadorEstancia cotizador;
    @Mock private RecepcionReservaExternaService recepcionExterna;
    @Mock private RechazoReservaExternaRepository rechazoRepository;
    @InjectMocks private CrearReservaUseCase useCase;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Estancia estancia = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 11, 2));
    private final List<Ocupante> ocupantes = List.of(titular);
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Apartamento apartamento = new Apartamento(apt, "Vista", 2, 4);
    private final ValorCongelado valor = new ValorCongelado(List.of(
            new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
            new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1)));
    private final PoliticaCancelacion politica = new PoliticaCancelacion(new VersionPolitica(2),
            LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 100)));

    @BeforeEach
    void setUp() {
        lenient().when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.of(apartamento));
        lenient().when(cotizador.cotizar(apt, estancia, ocupantes)).thenReturn(valor);
        lenient().when(politicaRepository.vigenteEn(ahora.toLocalDate())).thenReturn(Optional.of(politica));
        lenient().when(generadorCodigo.generar(ahora.toLocalDate())).thenReturn(codigo);
        lenient().when(reservaRepository.guardar(any(Reserva.class))).then(returnsFirstArg());
    }

    @Test
    void creaLaReservaPendienteConValorYPoliticaCongeladosYAbreSuFolio() {
        Reserva reserva = useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.PORTAL, null, ahora);

        assertEquals(codigo, reserva.getCodigo());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals(valor, reserva.getValor());
        assertEquals(new VersionPolitica(2), reserva.getPolitica());
        assertNull(reserva.getHoraEstimadaLlegada());

        ArgumentCaptor<Folio> folio = ArgumentCaptor.forClass(Folio.class);
        InOrder orden = inOrder(disponibilidad, reservaRepository, folioRepository);
        orden.verify(disponibilidad).verificarDisponible(apartamento, estancia, 1, null);
        orden.verify(reservaRepository).guardar(reserva);
        orden.verify(folioRepository).guardar(folio.capture());
        assertEquals(codigo, folio.getValue().getReserva());
        assertEquals(Dinero.de(200_000), folio.getValue().saldo());
    }

    @Test
    void registraLaHoraEstimadaDeLlegadaCuandoSeInforma() {
        Reserva reserva = useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.DIRECTO,
                LocalTime.of(16, 0), ahora);

        assertEquals(LocalTime.of(16, 0), reserva.getHoraEstimadaLlegada());
    }

    @Test
    void lanzaNoEncontradoSiElApartamentoNoExisteYNoGuardaNada() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.PORTAL, null, ahora));

        verifyNoInteractions(disponibilidad, cotizador, reservaRepository, folioRepository);
    }

    @Test
    void lanzaNoEncontradoSiNoHayPoliticaVigenteYNoGuardaNada() {
        when(politicaRepository.vigenteEn(ahora.toLocalDate())).thenReturn(Optional.empty());

        assertThrows(PoliticaCancelacionNoEncontradaException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.PORTAL, null, ahora));

        verifyNoInteractions(reservaRepository, folioRepository);
    }

    @Test
    void sePropagaLaReglaDeDisponibilidadYNoSeCotizaNiSeGuarda() {
        doThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"))
                .when(disponibilidad).verificarDisponible(apartamento, estancia, 1, null);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.PORTAL, null, ahora));

        verifyNoInteractions(cotizador, reservaRepository, folioRepository);
    }

    @Test
    void noPermiteReservarHaciaElPasadoRN04YNoAbreFolio() {
        Estancia pasada = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        when(cotizador.cotizar(apt, pasada, ocupantes)).thenReturn(new ValorCongelado(List.of(
                new CargoNoche(LocalDate.of(2026, 10, 1), "BASE", Dinero.de(50_000), 1),
                new CargoNoche(LocalDate.of(2026, 10, 2), "BASE", Dinero.de(50_000), 1))));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, pasada, titular, ocupantes, CanalOrigen.PORTAL, null, ahora));

        assertEquals("La fecha de entrada no puede ser anterior a la fecha actual", ex.getMessage());
        verifyNoInteractions(folioRepository);
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void elTitularDebeSerUnoDeLosOcupantes() {
        Ocupante otro = new Ocupante(new DocumentoIdentidad("CC-200"), "Ana",
                LocalDate.of(1992, 5, 5), LocalDate.of(2026, 11, 2));
        when(cotizador.cotizar(apt, estancia, List.of(otro))).thenReturn(valor);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, estancia, titular, List.of(otro), CanalOrigen.PORTAL, null, ahora));

        verifyNoInteractions(folioRepository);
    }

    // --- RN-19 y RN-18: canal externo ---

    private final IdentificadorExterno mensaje = new IdentificadorExterno("BKG-778899");

    @Test
    void laReservaExternaGuardaSuIdentificadorYSeVerificaLaRecepcionAntesQueLaDisponibilidad() {
        Reserva reserva = useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.EXTERNO, mensaje, null, ahora);

        assertEquals(mensaje, reserva.getIdentificadorExterno());
        assertEquals(CanalOrigen.EXTERNO, reserva.getCanalOrigen());
        InOrder orden = inOrder(recepcionExterna, disponibilidad);
        orden.verify(recepcionExterna).verificarRecepcion(CanalOrigen.EXTERNO, mensaje);
        orden.verify(disponibilidad).verificarDisponible(apartamento, estancia, 1, null);
        verifyNoInteractions(rechazoRepository);
    }

    @Test
    void unMensajeRepetidoRN19SeRechazaSinCrearNadaNiRegistrarUnRechazoPorConflicto() {
        doThrow(new ReglaDominioException("El mensaje ya fue recibido"))
                .when(recepcionExterna).verificarRecepcion(CanalOrigen.EXTERNO, mensaje);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.EXTERNO, mensaje, null, ahora));

        verifyNoInteractions(disponibilidad, cotizador, rechazoRepository, folioRepository);
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void unaReservaExternaEnConflictoRN18SeRechazaSeRegistraYNoToqueLaVigente() {
        doThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"))
                .when(disponibilidad).verificarDisponible(apartamento, estancia, 1, null);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.EXTERNO, mensaje, null, ahora));

        assertTrue(error.getMessage().contains("solapa"));
        ArgumentCaptor<RechazoReservaExterna> captor = ArgumentCaptor.forClass(RechazoReservaExterna.class);
        verify(rechazoRepository).guardar(captor.capture());
        RechazoReservaExterna rechazo = captor.getValue();
        assertEquals(CanalOrigen.EXTERNO, rechazo.getCanalOrigen());
        assertEquals(mensaje, rechazo.getIdentificadorExterno());
        assertEquals(apt, rechazo.getApartamento());
        assertEquals(estancia, rechazo.getEstancia());
        assertEquals(error.getMessage(), rechazo.getMotivo());
        assertEquals(ahora, rechazo.getRechazadaEn());
        verify(reservaRepository, never()).guardar(any());   // la reserva vigente no se sobrescribe
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void cualquierReglaQueViolaUnaReservaExternaQuedaRegistradaComoRechazo() {
        Estancia pasada = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        when(cotizador.cotizar(apt, pasada, ocupantes)).thenReturn(new ValorCongelado(List.of(
                new CargoNoche(LocalDate.of(2026, 10, 1), "ALTA", Dinero.de(100_000), 1),
                new CargoNoche(LocalDate.of(2026, 10, 2), "ALTA", Dinero.de(100_000), 1))));

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, pasada, titular, ocupantes, CanalOrigen.EXTERNO, mensaje, null, ahora));

        verify(rechazoRepository).guardar(any(RechazoReservaExterna.class));
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void elConflictoEnUnCanalSinIdentificadorExternoNoGeneraRechazoRegistrado() {
        doThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"))
                .when(disponibilidad).verificarDisponible(apartamento, estancia, 1, null);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.PORTAL, null, ahora));

        verifyNoInteractions(rechazoRepository);
    }

    @Test
    void siElApartamentoDeLaReservaExternaNoExisteEsUn404YNoUnRechazoPorConflicto() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class,
                () -> useCase.ejecutar(apt, estancia, titular, ocupantes, CanalOrigen.EXTERNO, mensaje, null, ahora));

        verifyNoInteractions(rechazoRepository);
    }
}
