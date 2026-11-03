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

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** Confirmar reserva (con anticipo) y CU-08 Registrar pago en folio. */
@ExtendWith(MockitoExtension.class)
class ConfirmarYPagarUseCaseTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private FolioRepository folioRepository;
    @Mock private ConfiguracionAlojamiento configuracion;

    private ConfirmarReservaUseCase confirmar;
    private RegistrarPagoUseCase pagar;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 2, 10, 0);
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
    private Reserva reserva;
    private Folio folio;

    @BeforeEach
    void setUp() {
        confirmar = new ConfirmarReservaUseCase(reservaRepository, folioRepository,
                new ConfirmacionReservaService(configuracion));
        pagar = new RegistrarPagoUseCase(folioRepository);
        reserva = Reserva.crear(codigo, new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), ahora);
        folio = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        lenient().when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        lenient().when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.of(folio));
        lenient().when(reservaRepository.guardar(any(Reserva.class))).then(returnsFirstArg());
        lenient().when(folioRepository.guardar(any(Folio.class))).then(returnsFirstArg());
        lenient().when(configuracion.anticipoPorcentaje()).thenReturn(30);
    }

    // ---------- Confirmar ----------

    @Test
    void confirmaConLaHoraInformadaCuandoElAnticipoEstaPagado() {
        folio.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(60_000)));

        Reserva resultado = confirmar.ejecutar(codigo, LocalTime.of(16, 0), "Juan", ahora);

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        assertEquals(LocalTime.of(16, 0), resultado.getHoraEstimadaLlegada());
        verify(reservaRepository).guardar(reserva);
    }

    @Test
    void rechazaConfirmarSinAnticipoYNoGuardaNada() {
        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(codigo, LocalTime.of(16, 0), "Juan", ahora));

        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void rechazaConfirmarSinHoraEstimadaRN09AunConAnticipo() {
        folio.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(200_000)));

        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(codigo, null, "Juan", ahora));

        verify(reservaRepository, never()).guardar(any());
    }

    @Test
    void usaLaHoraQueLaReservaYaTieneSiNoSeInformaOtra() {
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(14, 0), "Juan", ahora);
        folio.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(60_000)));

        Reserva resultado = confirmar.ejecutar(codigo, null, "Juan", ahora);

        assertEquals(LocalTime.of(14, 0), resultado.getHoraEstimadaLlegada());
        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
    }

    @Test
    void lanzaNoEncontradoSiFaltaLaReservaOElFolio() {
        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.empty());
        assertThrows(ReservaNoEncontradaException.class, () -> confirmar.ejecutar(codigo, null, "Juan", ahora));

        when(reservaRepository.buscarPorCodigo(codigo)).thenReturn(Optional.of(reserva));
        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());
        assertThrows(FolioNoEncontradoException.class, () -> confirmar.ejecutar(codigo, null, "Juan", ahora));

        verify(reservaRepository, never()).guardar(any());
    }

    // ---------- CU-08 Registrar pago ----------

    @Test
    void registraElPagoYElSaldoBajaEnConsecuencia() {
        Pago pago = new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(80_000));

        Folio resultado = pagar.ejecutar(codigo, pago);

        assertEquals(List.of(pago), resultado.obtenerPagos());
        assertEquals(Dinero.de(120_000), resultado.saldo());
        verify(folioRepository).guardar(folio);
    }

    @Test
    void admiteVariosPagosConMediosDistintos() {
        pagar.ejecutar(codigo, new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(50_000)));
        Folio resultado = pagar.ejecutar(codigo, new Pago("TARJETA", LocalDate.of(2026, 11, 4), Dinero.de(30_000)));

        assertEquals(2, resultado.obtenerPagos().size());
        assertEquals(Dinero.de(120_000), resultado.saldo());
    }

    @Test
    void noRegistraPagosSobreUnFolioInexistenteOCerrado() {
        folio.cerrar("Autorizado");
        assertThrows(ReglaDominioException.class,
                () -> pagar.ejecutar(codigo, new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(1_000))));
        verify(folioRepository, never()).guardar(any());

        when(folioRepository.buscarPorReserva(codigo)).thenReturn(Optional.empty());
        assertThrows(FolioNoEncontradoException.class,
                () -> pagar.ejecutar(codigo, new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(1_000))));
    }
}
