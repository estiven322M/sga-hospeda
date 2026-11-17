package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** 7.6: registrar una novedad y consultar el historial de un apartamento. Los casos de uso solo orquestan. */
@ExtendWith(MockitoExtension.class)
class NovedadUseCasesTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2027, 3, 10, 10, 0);

    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private NovedadRepository novedadRepository;

    private final Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");

    private RegistrarNovedadUseCase registrar() {
        return new RegistrarNovedadUseCase(apartamentoRepository, novedadRepository, reloj);
    }

    @Test
    void registraLaNovedadConLaFechaDelRelojYElAutorRecibido() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.of(mock(Apartamento.class)));
        when(novedadRepository.registrar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Novedad novedad = registrar().ejecutar(apt, "Personal de Servicio", "Fuga de agua", GravedadNovedad.ALTA);

        assertEquals(new Novedad(apt, AHORA, "Personal de Servicio", "Fuga de agua", GravedadNovedad.ALTA), novedad);
        verify(novedadRepository).registrar(novedad);
    }

    @Test
    void noRegistraSobreUnApartamentoQueNoExiste() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class,
                () -> registrar().ejecutar(apt, "Personal de Servicio", "Fuga de agua", GravedadNovedad.ALTA));
        verify(novedadRepository, never()).registrar(any());
    }

    @Test
    void siElDominioRechazaLaNovedadNoSeGuardaNada() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.of(mock(Apartamento.class)));

        assertThrows(ReglaDominioException.class,
                () -> registrar().ejecutar(apt, "Personal de Servicio", "a".repeat(501), GravedadNovedad.ALTA));
        verify(novedadRepository, never()).registrar(any());
    }

    @Test
    void listaElHistorialDelApartamento() {
        SolicitudPagina pagina = new SolicitudPagina(1);
        Pagina<Novedad> historial = new Pagina<>(
                List.of(new Novedad(apt, AHORA, "Personal de Servicio", "Fuga de agua", GravedadNovedad.ALTA)), 1, 11);
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.of(mock(Apartamento.class)));
        when(novedadRepository.listarPorApartamento(apt, pagina)).thenReturn(historial);

        assertSame(historial, new ListarNovedadesUseCase(apartamentoRepository, novedadRepository).ejecutar(apt, pagina));
    }

    @Test
    void noListaElHistorialDeUnApartamentoQueNoExiste() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class,
                () -> new ListarNovedadesUseCase(apartamentoRepository, novedadRepository)
                        .ejecutar(apt, SolicitudPagina.primera()));
    }
}
