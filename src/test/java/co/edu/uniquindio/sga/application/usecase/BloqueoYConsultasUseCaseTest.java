package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.*;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.*;
import co.edu.uniquindio.sga.domain.service.RegistroBloqueoService;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** CU-09 (registrar y listar bloqueos) y las consultas de reservas y folio. */
@ExtendWith(MockitoExtension.class)
class BloqueoYConsultasUseCaseTest {

    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private BloqueoRepository bloqueoRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private RegistroBloqueoService registroBloqueoService;

    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Estancia rango = new Estancia(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 23));
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private Reserva reserva() {
        return Reserva.crear(codigo, apt, new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                titular, List.of(titular), CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    // ---------- CU-09 Registrar bloqueo ----------

    @Test
    void registraElBloqueoCreadoPorElDominio() {
        Bloqueo bloqueo = new Bloqueo(apt, rango, "Mantenimiento");
        when(apartamentoRepository.buscarPorIdentificacion(apt))
                .thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(registroBloqueoService.crear(apt, rango, "Mantenimiento")).thenReturn(bloqueo);
        when(bloqueoRepository.guardar(bloqueo)).thenReturn(bloqueo);

        Bloqueo resultado = new RegistrarBloqueoUseCase(apartamentoRepository, bloqueoRepository,
                registroBloqueoService).ejecutar(apt, rango, "Mantenimiento");

        assertSame(bloqueo, resultado);
        verify(bloqueoRepository).guardar(bloqueo);
    }

    @Test
    void noRegistraBloqueoSiElApartamentoNoExiste() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class, () -> new RegistrarBloqueoUseCase(
                apartamentoRepository, bloqueoRepository, registroBloqueoService).ejecutar(apt, rango, "x"));

        verifyNoInteractions(registroBloqueoService, bloqueoRepository);
    }

    @Test
    void noGuardaElBloqueoSiElDominioLoRechaza() {
        when(apartamentoRepository.buscarPorIdentificacion(apt))
                .thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(registroBloqueoService.crear(apt, rango, "x"))
                .thenThrow(new ReglaDominioException("No se puede bloquear: reserva activa"));

        assertThrows(ReglaDominioException.class, () -> new RegistrarBloqueoUseCase(
                apartamentoRepository, bloqueoRepository, registroBloqueoService).ejecutar(apt, rango, "x"));

        verify(bloqueoRepository, never()).guardar(any());
    }

    // ---------- Listar bloqueos ----------

    @Test
    void listaLosBloqueosPaginadosDeUnApartamentoExistente() {
        Pagina<Bloqueo> pagina = new Pagina<>(List.of(new Bloqueo(apt, rango, "Pintura")), 0, 1);
        when(apartamentoRepository.buscarPorIdentificacion(apt))
                .thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(bloqueoRepository.listarPorApartamento(apt, SolicitudPagina.primera())).thenReturn(pagina);

        assertSame(pagina, new ListarBloqueosUseCase(apartamentoRepository, bloqueoRepository)
                .ejecutar(apt, SolicitudPagina.primera()));
    }

    @Test
    void noListaBloqueosDeUnApartamentoInexistente() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class, () -> new ListarBloqueosUseCase(
                apartamentoRepository, bloqueoRepository).ejecutar(apt, SolicitudPagina.primera()));

        verifyNoInteractions(bloqueoRepository);
    }

    // ---------- Consultar reserva ----------

    @Test
    void consultaLaReservaPorSuCodigo() {
        Reserva reserva = reserva();
        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));

        assertSame(reserva, new ConsultarReservaUseCase(reservaRepository).ejecutar(codigo));
    }

    @Test
    void consultaDeReservaInexistenteEsNoEncontrada() {
        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class,
                () -> new ConsultarReservaUseCase(reservaRepository).ejecutar(codigo));
    }

    // ---------- Listar reservas ----------

    @Test
    void listaLasReservasConElFiltroYLaPaginaPedidos() {
        FiltroReservas filtro = new FiltroReservas(null, null, EstadoReserva.PENDIENTE, apt, CanalOrigen.PORTAL, null);
        SolicitudPagina pagina = new SolicitudPagina(2);
        Pagina<Reserva> esperada = new Pagina<>(List.of(reserva()), 2, 21);
        when(reservaRepository.buscar(filtro, pagina)).thenReturn(esperada);

        assertSame(esperada, new ListarReservasUseCase(reservaRepository).ejecutar(filtro, pagina));
        verify(reservaRepository).buscar(filtro, pagina);
    }

    // ---------- Consultar folio ----------

    @Test
    void consultaElFolioDeLaReservaConSuSaldo() {
        Folio folio = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.of(folio));

        Folio resultado = new ConsultarFolioUseCase(folioRepository).ejecutar(codigo);

        assertEquals(Dinero.de(200_000), resultado.saldo());
    }

    @Test
    void consultaDeFolioInexistenteEsNoEncontrada() {
        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());

        assertThrows(FolioNoEncontradoException.class,
                () -> new ConsultarFolioUseCase(folioRepository).ejecutar(codigo));
    }
}
