package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-21: vencimiento automático de las reservas pendientes que superan el plazo de confirmación. */
@ExtendWith(MockitoExtension.class)
class VencerReservasPendientesUseCaseTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private ConfiguracionAlojamiento configuracion;
    private VencerReservasPendientesUseCase useCase;

    private final LocalDateTime creada = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final LocalDateTime ahora = creada.plusHours(30); // 6 h más allá del plazo de 24 h
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    @BeforeEach
    void setUp() {
        useCase = new VencerReservasPendientesUseCase(reservaRepository, folioRepository, configuracion);
        lenient().when(configuracion.plazoConfirmacion()).thenReturn(new PlazoConfirmacion(24));
        lenient().when(reservaRepository.guardar(any(Reserva.class))).then(returnsFirstArg());
        lenient().when(folioRepository.guardar(any(Folio.class))).then(returnsFirstArg());
    }

    private Reserva pendiente(String codigo, LocalDateTime creadaEn) {
        return Reserva.crear(new CodigoReserva(codigo), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), creadaEn);
    }

    private Folio folioDe(Reserva r) {
        return new Folio(r.getCodigo(), new Cargo("Alojamiento", Dinero.de(200_000)));
    }

    @Test
    void consultaLasPendientesCreadasAntesDelLimiteQueCalculaElPlazo() {
        when(reservaRepository.buscarPendientesCreadasAntesDe(any())).thenReturn(List.of());

        int vencidas = useCase.ejecutar(ahora);

        assertEquals(0, vencidas);
        verify(reservaRepository).buscarPendientesCreadasAntesDe(creada.plusHours(6)); // ahora - 24 h
        verify(reservaRepository, never()).guardar(any());
        verifyNoInteractions(folioRepository);
    }

    @Test
    void cancelaCadaReservaVencidaRevierteSuFolioYCuentaCuantasFueron() {
        Reserva a = pendiente("RES-2026-00001", creada), b = pendiente("RES-2026-00002", creada.minusHours(3));
        Folio fa = folioDe(a), fb = folioDe(b);
        when(reservaRepository.buscarPendientesCreadasAntesDe(any())).thenReturn(List.of(a, b));
        when(folioRepository.buscarPorReserva(a.getCodigo())).thenReturn(Optional.of(fa));
        when(folioRepository.buscarPorReserva(b.getCodigo())).thenReturn(Optional.of(fb));

        int vencidas = useCase.ejecutar(ahora);

        assertEquals(2, vencidas);
        assertEquals(EstadoReserva.CANCELADA, a.getEstado());
        assertEquals(EstadoReserva.CANCELADA, b.getEstado());
        assertEquals(Dinero.CERO, fa.saldo()); // sin penalidad
        verify(reservaRepository).guardar(a);
        verify(reservaRepository).guardar(b);
        verify(folioRepository).guardar(fa);
        verify(folioRepository).guardar(fb);
    }

    @Test
    void unPagoParcialQuedaComoSaldoAFavorAlVencer() {
        Reserva r = pendiente("RES-2026-00001", creada);
        Folio f = folioDe(r);
        f.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 2), Dinero.de(50_000)));
        when(reservaRepository.buscarPendientesCreadasAntesDe(any())).thenReturn(List.of(r));
        when(folioRepository.buscarPorReserva(r.getCodigo())).thenReturn(Optional.of(f));

        useCase.ejecutar(ahora);

        assertEquals(Dinero.de(-50_000), f.saldo());
    }

    @Test
    void ignoraLasQueElPuertoDevuelvaPeroTodaviaEstanDentroDelPlazo() {
        Reserva reciente = pendiente("RES-2026-00003", ahora.minusHours(2)); // el puerto se equivocó: no ha vencido
        when(reservaRepository.buscarPendientesCreadasAntesDe(any())).thenReturn(List.of(reciente));

        int vencidas = useCase.ejecutar(ahora);

        assertEquals(0, vencidas);
        assertEquals(EstadoReserva.PENDIENTE, reciente.getEstado());
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void esIdempotenteUnaVezCanceladaYaNoVuelveAAparecer() {
        Reserva r = pendiente("RES-2026-00001", creada);
        when(reservaRepository.buscarPendientesCreadasAntesDe(any())).thenReturn(List.of(r)).thenReturn(List.of());
        when(folioRepository.buscarPorReserva(r.getCodigo())).thenReturn(Optional.of(folioDe(r)));

        assertEquals(1, useCase.ejecutar(ahora));
        assertEquals(0, useCase.ejecutar(ahora.plusMinutes(5)));
        verify(reservaRepository, times(1)).guardar(r);
    }

    @Test
    void siFaltaElFolioDeUnaReservaLaEjecucionFallaYSePropagaParaRevertirLaTransaccion() {
        Reserva r = pendiente("RES-2026-00001", creada);
        when(reservaRepository.buscarPendientesCreadasAntesDe(any())).thenReturn(List.of(r));
        when(folioRepository.buscarPorReserva(r.getCodigo())).thenReturn(Optional.empty());

        assertThrows(FolioNoEncontradoException.class, () -> useCase.ejecutar(ahora));

        assertEquals(EstadoReserva.PENDIENTE, r.getEstado()); // el folio se busca antes de cancelar
        verify(reservaRepository, never()).guardar(any());
    }
}
