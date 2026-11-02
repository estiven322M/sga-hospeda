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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RetencionCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.*;

@ExtendWith(MockitoExtension.class)
class CancelarReservaUseCaseTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private PoliticaCancelacionRepository politicaRepository;
    @Mock private ConfiguracionAlojamiento configuracion;
    private RetencionCancelacionService retencionService;
    private CancelarReservaUseCase useCase;

    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final LocalDateTime ahora = LocalDateTime.of(2026, 12, 8, 12, 0); // ~51 h antes de la entrada
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private final PoliticaCancelacion politica = new PoliticaCancelacion(new VersionPolitica(2),
            LocalDate.of(2026, 1, 1),
            List.of(new TramoRetencion(72, 0), new TramoRetencion(24, 50), new TramoRetencion(0, 100)));
    private Reserva reserva;
    private Folio folio;

    @BeforeEach
    void setUp() {
        retencionService = new RetencionCancelacionService(configuracion);
        useCase = new CancelarReservaUseCase(reservaRepository, folioRepository, politicaRepository, retencionService);
        reserva = Reserva.crear(codigo, new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL,
                new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(2), LocalDateTime.of(2026, 11, 2, 10, 0));
        folio = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        lenient().when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        lenient().when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.of(folio));
        lenient().when(politicaRepository.porVersion(new VersionPolitica(2))).thenReturn(Optional.of(politica));
        lenient().when(configuracion.horaEntrada()).thenReturn(LocalTime.of(15, 0));
        lenient().when(reservaRepository.guardar(any(Reserva.class))).then(returnsFirstArg());
    }

    @Test
    void cancelaLaReservaYLiquidaElFolioConLaPoliticaCongelada() {
        Reserva resultado = useCase.ejecutar(codigo, "Cambio de planes", "Juan", ahora);

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
        assertEquals(Dinero.de(100_000), folio.saldo()); // 50 % retenido; el huésped no había pagado nada
        verify(reservaRepository).guardar(reserva);
        verify(folioRepository).guardar(folio);
    }

    @Test
    void usaLaPoliticaDeLaVersionCongeladaNoLaVigente() {
        useCase.ejecutar(codigo, "Cambio de planes", "Juan", ahora);

        verify(politicaRepository).porVersion(new VersionPolitica(2));
        verify(politicaRepository, never()).vigenteEn(any());
    }

    @Test
    void lanzaNoEncontradoSiLaReservaNoExisteYNoGuardaNada() {
        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class, () -> useCase.ejecutar(codigo, "x", "Juan", ahora));

        verifyNoInteractions(folioRepository, politicaRepository);
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void lanzaNoEncontradoSiNoHayFolioYNoGuardaNada() {
        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());

        assertThrows(FolioNoEncontradoException.class, () -> useCase.ejecutar(codigo, "x", "Juan", ahora));

        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void lanzaNoEncontradoSiNoExisteLaVersionDeLaPoliticaCongelada() {
        when(politicaRepository.porVersion(new VersionPolitica(2))).thenReturn(Optional.empty());

        assertThrows(PoliticaCancelacionNoEncontradaException.class,
                () -> useCase.ejecutar(codigo, "x", "Juan", ahora));

        verify(reservaRepository, never()).guardar(any());
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void noPermiteCancelarUnaReservaYaCanceladaRN08YNoTocaElFolio() {
        reserva.cancelar("Primera cancelación", "Juan", ahora.minusDays(1));
        Dinero saldoAntes = folio.saldo();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(codigo, "Otra vez", "Juan", ahora));

        assertEquals(saldoAntes, folio.saldo());
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void todaCancelacionExigeMotivo() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(codigo, " ", "Juan", ahora));

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        verify(reservaRepository, never()).guardar(any());
    }
}
