package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.*;

@ExtendWith(MockitoExtension.class)
class ConsultarDisponibilidadUseCaseTest {

    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private DisponibilidadApartamentoService disponibilidad;
    @Mock private CotizadorEstancia cotizador;
    @InjectMocks private ConsultarDisponibilidadUseCase useCase;

    private final Estancia estancia = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    private final List<Ocupante> ocupantes = List.of(new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 11, 2)));

    private Apartamento apt(String id, int capacidad) {
        return new Apartamento(new IdentificacionApartamento(id), "Apto " + id, 1, capacidad);
    }

    private ValorCongelado cotizacion(long porNoche) {
        return new ValorCongelado(List.of(
                new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(porNoche), 1),
                new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(porNoche), 1)));
    }

    @Test
    void devuelveSoloLosApartamentosQueElDominioDeclaraDisponiblesOrdenadosPorPrecio() {
        Apartamento caro = apt("APT-1", 4), barato = apt("APT-2", 4), ocupado = apt("APT-3", 4);
        when(apartamentoRepository.listarActivos()).thenReturn(List.of(caro, barato, ocupado));
        // lenient: el mismo mock se invoca también con los apartamentos libres (que no lanzan nada)
        lenient().doThrow(new ReglaDominioException("solapa")).when(disponibilidad)
                .verificarDisponible(ocupado, estancia, 1, null);
        when(cotizador.cotizar(caro.getIdentificacion(), estancia, ocupantes)).thenReturn(cotizacion(200_000));
        when(cotizador.cotizar(barato.getIdentificacion(), estancia, ocupantes)).thenReturn(cotizacion(100_000));

        Pagina<ApartamentoDisponible> pagina =
                useCase.ejecutar(estancia, ocupantes, null, null, null, SolicitudPagina.primera());

        assertEquals(List.of(barato, caro),
                pagina.contenido().stream().map(ApartamentoDisponible::apartamento).toList());
        verify(cotizador, never()).cotizar(eq(ocupado.getIdentificacion()), any(), any());
    }

    @Test
    void filtraPorCapacidadMinimaSinConsultarLaDisponibilidadDeLosDemas() {
        Apartamento pequeno = apt("APT-1", 2), grande = apt("APT-2", 6);
        when(apartamentoRepository.listarActivos()).thenReturn(List.of(pequeno, grande));
        when(cotizador.cotizar(grande.getIdentificacion(), estancia, ocupantes)).thenReturn(cotizacion(100_000));

        Pagina<ApartamentoDisponible> pagina =
                useCase.ejecutar(estancia, ocupantes, 4, null, null, SolicitudPagina.primera());

        assertEquals(1, pagina.totalElementos());
        assertEquals(grande, pagina.contenido().get(0).apartamento());
        verify(disponibilidad, never()).verificarDisponible(eq(pequeno), any(), anyInt(), any());
    }

    @Test
    void filtraPorRangoDeDineroDelTotalDeLaEstancia() {
        Apartamento a = apt("APT-1", 4), b = apt("APT-2", 4), c = apt("APT-3", 4);
        when(apartamentoRepository.listarActivos()).thenReturn(List.of(a, b, c));
        when(cotizador.cotizar(a.getIdentificacion(), estancia, ocupantes)).thenReturn(cotizacion(50_000));   // 100.000
        when(cotizador.cotizar(b.getIdentificacion(), estancia, ocupantes)).thenReturn(cotizacion(100_000));  // 200.000
        when(cotizador.cotizar(c.getIdentificacion(), estancia, ocupantes)).thenReturn(cotizacion(200_000));  // 400.000

        Pagina<ApartamentoDisponible> pagina = useCase.ejecutar(estancia, ocupantes, null,
                Dinero.de(150_000), Dinero.de(400_000), SolicitudPagina.primera());

        assertEquals(List.of(b, c),
                pagina.contenido().stream().map(ApartamentoDisponible::apartamento).toList());
    }

    @Test
    void paginaDeDiezEnDiez() {
        List<Apartamento> doce = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> apt(String.format("APT-%02d", i), 4)).toList();
        when(apartamentoRepository.listarActivos()).thenReturn(doce);
        when(cotizador.cotizar(any(), eq(estancia), eq(ocupantes))).thenReturn(cotizacion(100_000));

        Pagina<ApartamentoDisponible> primera =
                useCase.ejecutar(estancia, ocupantes, null, null, null, new SolicitudPagina(0));
        Pagina<ApartamentoDisponible> segunda =
                useCase.ejecutar(estancia, ocupantes, null, null, null, new SolicitudPagina(1));

        assertEquals(10, primera.contenido().size());
        assertEquals(2, segunda.contenido().size());
        assertEquals(12, segunda.totalElementos());
    }

    @Test
    void sinApartamentosLibresDevuelveUnaPaginaVaciaSinCotizar() {
        Apartamento a = apt("APT-1", 4);
        when(apartamentoRepository.listarActivos()).thenReturn(List.of(a));
        doThrow(new ReglaDominioException("bloqueado")).when(disponibilidad)
                .verificarDisponible(a, estancia, 1, null);

        Pagina<ApartamentoDisponible> pagina =
                useCase.ejecutar(estancia, ocupantes, null, null, null, SolicitudPagina.primera());

        assertTrue(pagina.contenido().isEmpty());
        verifyNoInteractions(cotizador);
    }

    @Test
    void unaComposicionDeGrupoInvalidaSePropagaComoReglaDeDominio() {
        Apartamento a = apt("APT-1", 4);
        when(apartamentoRepository.listarActivos()).thenReturn(List.of(a));
        when(cotizador.cotizar(a.getIdentificacion(), estancia, ocupantes))
                .thenThrow(new ReglaDominioException("La reserva debe tener al menos un ocupante facturable"));

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(estancia, ocupantes, null, null, null, SolicitudPagina.primera()));
    }
}
