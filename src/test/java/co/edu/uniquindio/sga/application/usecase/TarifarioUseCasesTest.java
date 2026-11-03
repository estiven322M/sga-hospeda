package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.*;
import co.edu.uniquindio.sga.domain.service.RegistroPoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.service.RegistroTarifaService;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** Temporadas (calendario válido), política de cancelación versionada y tarifas por apartamento y temporada. */
@ExtendWith(MockitoExtension.class)
class TarifarioUseCasesTest {

    @Mock private TemporadaRepository temporadas;
    @Mock private PoliticaCancelacionRepository politicas;
    @Mock private ApartamentoRepository apartamentos;
    @Mock private TarifarioRepository tarifario;

    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Temporada base = new Temporada("BASE", true, List.of());
    private final Temporada alta = new Temporada("ALTA", false,
            List.of(new Periodo(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15))));

    @BeforeEach
    void setUp() {
        lenient().when(temporadas.guardar(any(Temporada.class))).then(returnsFirstArg());
        lenient().when(politicas.guardar(any(PoliticaCancelacion.class))).then(returnsFirstArg());
    }

    // ---------- Temporadas ----------

    @Test
    void registraLaTemporadaBaseYDespuesLasDemasValidandoElCalendario() {
        when(temporadas.listarTodas()).thenReturn(List.of());
        RegistrarTemporadaUseCase useCase = new RegistrarTemporadaUseCase(temporadas);
        assertSame(base, useCase.ejecutar(base));

        when(temporadas.listarTodas()).thenReturn(List.of(base));
        assertSame(alta, useCase.ejecutar(alta));
    }

    @Test
    void rechazaUnaTemporadaQueSeSolapaYNoGuardaNada() {
        when(temporadas.listarTodas()).thenReturn(List.of(base, alta));
        Temporada feriado = new Temporada("FERIADO", false,
                List.of(new Periodo(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 20))));

        assertThrows(ReglaDominioException.class, () -> new RegistrarTemporadaUseCase(temporadas).ejecutar(feriado));

        verify(temporadas, never()).guardar(any());
    }

    @Test
    void rechazaUnaSegundaBaseUnNombreRepetidoYUnaTemporadaSinBaseExistente() {
        var useCase = new RegistrarTemporadaUseCase(temporadas);

        when(temporadas.listarTodas()).thenReturn(List.of(base));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(new Temporada("OTRA BASE", true, List.of())));
        when(temporadas.listarTodas()).thenReturn(List.of(base, alta));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(alta));
        when(temporadas.listarTodas()).thenReturn(List.of());
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(alta)); // la base va primero

        verify(temporadas, never()).guardar(any());
    }

    @Test
    void listaLasTemporadasDeDiezEnDiez() {
        List<Temporada> doce = IntStream.rangeClosed(1, 12).mapToObj(i -> new Temporada("T" + i, false,
                List.of(new Periodo(LocalDate.of(2027, 1, 1).plusDays(i * 2), LocalDate.of(2027, 1, 1).plusDays(i * 2))))).toList();
        when(temporadas.listarTodas()).thenReturn(doce);

        var pagina = new ListarTemporadasUseCase(temporadas).ejecutar(new SolicitudPagina(1));

        assertEquals(2, pagina.contenido().size());
        assertEquals(12, pagina.totalElementos());
    }

    // ---------- Política ----------

    @Test
    void registraUnaNuevaVersionDeLaPolitica() {
        when(politicas.ultimaVersion()).thenReturn(Optional.empty());
        var useCase = new RegistrarPoliticaCancelacionUseCase(politicas, new RegistroPoliticaCancelacionService(politicas));

        PoliticaCancelacion p = useCase.ejecutar(LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 100)));

        assertEquals(new VersionPolitica(1), p.getVersion());
        verify(politicas).guardar(p);
    }

    @Test
    void noGuardaUnaPoliticaInvalida() {
        when(politicas.ultimaVersion()).thenReturn(Optional.empty());
        var useCase = new RegistrarPoliticaCancelacionUseCase(politicas, new RegistroPoliticaCancelacionService(politicas));

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(0, 100))));

        verify(politicas, never()).guardar(any());
    }

    @Test
    void consultaLaPoliticaVigenteYUnaVersionLanzandoNoEncontrado() {
        PoliticaCancelacion p = new PoliticaCancelacion(new VersionPolitica(2), LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 100)));
        when(politicas.vigenteEn(LocalDate.of(2026, 11, 2))).thenReturn(Optional.of(p));
        when(politicas.porVersion(new VersionPolitica(2))).thenReturn(Optional.of(p));
        when(politicas.vigenteEn(LocalDate.of(2025, 1, 1))).thenReturn(Optional.empty());
        when(politicas.porVersion(new VersionPolitica(9))).thenReturn(Optional.empty());

        assertSame(p, new ConsultarPoliticaVigenteUseCase(politicas).ejecutar(LocalDate.of(2026, 11, 2)));
        assertSame(p, new ConsultarPoliticaPorVersionUseCase(politicas).ejecutar(new VersionPolitica(2)));
        assertThrows(PoliticaCancelacionNoEncontradaException.class,
                () -> new ConsultarPoliticaVigenteUseCase(politicas).ejecutar(LocalDate.of(2025, 1, 1)));
        assertThrows(PoliticaCancelacionNoEncontradaException.class,
                () -> new ConsultarPoliticaPorVersionUseCase(politicas).ejecutar(new VersionPolitica(9)));
    }

    // ---------- Tarifas ----------

    @Test
    void guardaLaTarifaDeUnApartamentoExistenteEnUnaTemporadaExistente() {
        when(apartamentos.buscarPorIdentificacion(apt)).thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(temporadas.listarTodas()).thenReturn(List.of(base, alta));
        var useCase = new RegistrarTarifaUseCase(apartamentos, tarifario, new RegistroTarifaService(temporadas));

        Tarifa tarifa = useCase.ejecutar(apt, "ALTA", Dinero.de(120_000));

        assertEquals(new Tarifa("ALTA", Dinero.de(120_000)), tarifa);
        verify(tarifario).guardarTarifa(apt, tarifa);
    }

    @Test
    void noGuardaTarifasDeApartamentosOTemporadasInexistentes() {
        var useCase = new RegistrarTarifaUseCase(apartamentos, tarifario, new RegistroTarifaService(temporadas));

        when(apartamentos.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class, () -> useCase.ejecutar(apt, "ALTA", Dinero.de(1)));

        when(apartamentos.buscarPorIdentificacion(apt)).thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(temporadas.listarTodas()).thenReturn(List.of(base));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(apt, "ALTA", Dinero.de(1)));

        verify(tarifario, never()).guardarTarifa(any(), any());
    }

    @Test
    void listaLasTarifasDeUnApartamentoYLanzaNoEncontradoSiNoExiste() {
        when(apartamentos.buscarPorIdentificacion(apt)).thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(tarifario.listarTarifas(apt)).thenReturn(List.of(new Tarifa("BASE", Dinero.de(80_000))));
        var useCase = new ListarTarifasUseCase(apartamentos, tarifario);

        assertEquals(1, useCase.ejecutar(apt, SolicitudPagina.primera()).totalElementos());

        IdentificacionApartamento otro = new IdentificacionApartamento("APT-999");
        when(apartamentos.buscarPorIdentificacion(otro)).thenReturn(Optional.empty());
        assertThrows(ApartamentoNoEncontradoException.class, () -> useCase.ejecutar(otro, SolicitudPagina.primera()));
    }
}
