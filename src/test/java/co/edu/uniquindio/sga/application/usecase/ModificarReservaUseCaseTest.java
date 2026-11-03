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

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.*;

@ExtendWith(MockitoExtension.class)
class ModificarReservaUseCaseTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private DisponibilidadApartamentoService disponibilidad;
    @Mock private CotizadorEstancia cotizador;
    @InjectMocks private ModificarReservaUseCase useCase;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Apartamento apartamento = new Apartamento(apt, "Vista", 2, 4);
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private final Estancia tresNoches = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13));
    private Reserva reserva;
    private Folio folio;

    private ValorCongelado valor(int noches, long porNoche) {
        return new ValorCongelado(LocalDate.of(2026, 12, 10).datesUntil(LocalDate.of(2026, 12, 10).plusDays(noches))
                .map(d -> new CargoNoche(d, "ALTA", Dinero.de(porNoche), 1)).toList());
    }

    @BeforeEach
    void setUp() {
        reserva = Reserva.crear(codigo, apt, new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                titular, List.of(titular), CanalOrigen.PORTAL, valor(2, 100_000), new VersionPolitica(2),
                LocalDateTime.of(2026, 10, 30, 9, 0));
        folio = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        lenient().when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        lenient().when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.of(folio));
        lenient().when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.of(apartamento));
        lenient().when(reservaRepository.guardar(any(Reserva.class))).then(returnsFirstArg());
    }

    @Test
    void alAmpliarLaEstanciaRevalidaExcluyendoLaPropiaReservaYRegistraElAjustePositivo() {
        when(cotizador.cotizar(apt, tresNoches, List.of(titular))).thenReturn(valor(3, 100_000));

        Reserva resultado = useCase.ejecutar(codigo, tresNoches, List.of(titular), "Juan", ahora);

        assertEquals(tresNoches, resultado.getEstancia());
        assertEquals(new VersionPolitica(2), resultado.getPolitica());
        verify(disponibilidad).verificarDisponible(apartamento, tresNoches, 1, codigo);
        assertEquals(1, folio.obtenerAjustes().size());
        assertEquals(Dinero.de(100_000), folio.obtenerAjustes().get(0).valor());
        assertEquals(Dinero.de(300_000), folio.saldo());
        assertEquals(Dinero.de(200_000), folio.obtenerCargos().get(0).valor()); // el cargo original no se toca
        verify(reservaRepository).guardar(reserva);
        verify(folioRepository).guardar(folio);
    }

    @Test
    void siLaDiferenciaEsNegativaElAjusteEsNegativo() {
        Estancia unaNoche = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11));
        when(cotizador.cotizar(apt, unaNoche, List.of(titular))).thenReturn(valor(1, 100_000));

        useCase.ejecutar(codigo, unaNoche, List.of(titular), "Juan", ahora);

        assertEquals(Dinero.de(-100_000), folio.obtenerAjustes().get(0).valor());
        assertEquals(Dinero.de(100_000), folio.saldo());
    }

    @Test
    void siElValorNoCambiaNoSeRegistraAjuste() {
        Estancia mismasNoches = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
        when(cotizador.cotizar(apt, mismasNoches, List.of(titular))).thenReturn(valor(2, 100_000));

        useCase.ejecutar(codigo, mismasNoches, List.of(titular), "Juan", ahora);

        assertTrue(folio.obtenerAjustes().isEmpty());
        verify(folioRepository).guardar(folio);
    }

    @Test
    void lanzaNoEncontradoSiLaReservaElFolioOElApartamentoNoExisten() {
        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());
        assertThrows(ReservaNoEncontradaException.class,
                () -> useCase.ejecutar(codigo, tresNoches, List.of(titular), "Juan", ahora));

        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());
        assertThrows(FolioNoEncontradoException.class,
                () -> useCase.ejecutar(codigo, tresNoches, List.of(titular), "Juan", ahora));

        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.of(folio));
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class,
                () -> useCase.ejecutar(codigo, tresNoches, List.of(titular), "Juan", ahora));

        verifyNoInteractions(disponibilidad, cotizador);
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void siElDominioRechazaLaDisponibilidadNoSeCotizaNiSeGuardaNada() {
        doThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"))
                .when(disponibilidad).verificarDisponible(apartamento, tresNoches, 1, codigo);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(codigo, tresNoches, List.of(titular), "Juan", ahora));

        verifyNoInteractions(cotizador);
        verify(reservaRepository, never()).guardar(any());
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void noPermiteModificarUnaReservaQueYaIncioNiGuardaElFolio() {
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Juan", ahora);
        reserva.confirmar("Juan", ahora);
        reserva.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));
        when(cotizador.cotizar(apt, tresNoches, List.of(titular))).thenReturn(valor(3, 100_000));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(codigo, tresNoches, List.of(titular),
                "Juan", LocalDateTime.of(2026, 12, 10, 17, 0)));

        assertTrue(folio.obtenerAjustes().isEmpty());
        verify(folioRepository, never()).guardar(any());
    }

    @Test
    void siElFolioEstaCerradoLaModificacionFalla() {
        folio.cerrar("Autorizado");
        when(cotizador.cotizar(apt, tresNoches, List.of(titular))).thenReturn(valor(3, 100_000));

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(codigo, tresNoches, List.of(titular), "Juan", ahora));

        verify(reservaRepository, never()).guardar(any());
        verify(folioRepository, never()).guardar(any());
    }

    // --- RN-14: cambio de apartamento ---

    private final IdentificacionApartamento aptNuevo = new IdentificacionApartamento("APT-202");
    private final Apartamento destino = new Apartamento(aptNuevo, "Terraza", 2, 4);

    @Test
    void alCambiarDeApartamentoRevalidaYCotizaContraElNuevoYGuardaLaReserva() {
        when(apartamentoRepository.buscarPorIdentificacion(aptNuevo)).thenReturn(Optional.of(destino));
        when(cotizador.cotizar(aptNuevo, tresNoches, List.of(titular))).thenReturn(valor(3, 150_000));

        Reserva resultado = useCase.ejecutar(codigo, aptNuevo, tresNoches, List.of(titular), "Juan", ahora);

        assertEquals(aptNuevo, resultado.getApartamento());
        verify(disponibilidad).verificarDisponible(destino, tresNoches, 1, codigo);
        verify(disponibilidad, never()).verificarDisponible(eq(apartamento), any(), anyInt(), any());
        assertEquals(Dinero.de(250_000), folio.obtenerAjustes().get(0).valor()); // 450.000 - 200.000
        verify(reservaRepository).guardar(reserva);
        verify(folioRepository).guardar(folio);
    }

    @Test
    void sinIndicarApartamentoConservaElActual() {
        when(cotizador.cotizar(apt, tresNoches, List.of(titular))).thenReturn(valor(3, 100_000));

        Reserva resultado = useCase.ejecutar(codigo, null, tresNoches, List.of(titular), "Juan", ahora);

        assertEquals(apt, resultado.getApartamento());
        verify(apartamentoRepository, never()).buscarPorIdentificacion(aptNuevo);
    }

    @Test
    void siElApartamentoDestinoNoExisteLanzaNoEncontradoYNoGuardaNada() {
        when(apartamentoRepository.buscarPorIdentificacion(aptNuevo)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class,
                () -> useCase.ejecutar(codigo, aptNuevo, tresNoches, List.of(titular), "Juan", ahora));

        assertEquals(apt, reserva.getApartamento());
        verifyNoInteractions(disponibilidad, cotizador);
        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void siElDestinoNoEstaDisponibleOExcedeLaCapacidadElDominioLoRechazaYLaReservaQuedaIgual() {
        when(apartamentoRepository.buscarPorIdentificacion(aptNuevo)).thenReturn(Optional.of(destino));
        doThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"))
                .when(disponibilidad).verificarDisponible(destino, tresNoches, 1, codigo);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(codigo, aptNuevo, tresNoches, List.of(titular), "Juan", ahora));

        assertEquals(apt, reserva.getApartamento());
        verifyNoInteractions(cotizador);
        verify(reservaRepository, never()).guardar(any());
        verify(folioRepository, never()).guardar(any());
    }
}
