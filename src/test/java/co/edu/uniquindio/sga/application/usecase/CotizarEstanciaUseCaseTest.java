package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.valueobject.*;

@ExtendWith(MockitoExtension.class)
class CotizarEstanciaUseCaseTest {

    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private CotizadorEstancia cotizador;
    @InjectMocks private CotizarEstanciaUseCase useCase;

    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Estancia estancia = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    private final List<Ocupante> ocupantes = List.of(new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 11, 2)));

    @Test
    void cotizaConElServicioDeDominioCuandoElApartamentoExiste() {
        ValorCongelado esperado = new ValorCongelado(List.of(
                new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1)));
        when(apartamentoRepository.buscarPorIdentificacion(apt))
                .thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(cotizador.cotizar(apt, estancia, ocupantes)).thenReturn(esperado);

        ValorCongelado resultado = useCase.ejecutar(apt, estancia, ocupantes);

        assertSame(esperado, resultado);
        assertEquals(Dinero.de(200_000), resultado.total());
        verify(cotizador).cotizar(apt, estancia, ocupantes);
        verifyNoMoreInteractions(apartamentoRepository);
    }

    @Test
    void lanzaNoEncontradoYNoCotizaSiElApartamentoNoExiste() {
        when(apartamentoRepository.buscarPorIdentificacion(apt)).thenReturn(Optional.empty());

        assertThrows(ApartamentoNoEncontradoException.class, () -> useCase.ejecutar(apt, estancia, ocupantes));

        verifyNoInteractions(cotizador);
    }

    @Test
    void dejaPasarLaReglaDeDominioQueRechazaLaCotizacion() {
        when(apartamentoRepository.buscarPorIdentificacion(apt))
                .thenReturn(Optional.of(new Apartamento(apt, "Vista", 2, 4)));
        when(cotizador.cotizar(apt, estancia, ocupantes))
                .thenThrow(new ReglaDominioException("La reserva debe tener al menos un ocupante facturable"));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(apt, estancia, ocupantes));
    }
}
