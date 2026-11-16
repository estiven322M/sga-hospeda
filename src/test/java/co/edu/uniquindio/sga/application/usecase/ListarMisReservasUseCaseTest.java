package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/** "Mis reservas": el caso de uso solo arma el filtro por el titular que le dan y delega en el puerto. */
@ExtendWith(MockitoExtension.class)
class ListarMisReservasUseCaseTest {

    @Mock private ReservaRepository reservaRepository;
    @InjectMocks private ListarMisReservasUseCase useCase;

    private final DocumentoIdentidad titular = new DocumentoIdentidad("1094000111");

    @Test
    void filtraPorElDocumentoDelTitularYDelegaElOrdenYLaPaginacionEnElPuerto() {
        FiltroReservas esperado = new FiltroReservas(null, null, null, null, null, titular);
        Pagina<Reserva> pagina = new Pagina<>(List.of(), 0, 0);
        when(reservaRepository.buscar(esperado, SolicitudPagina.primera())).thenReturn(pagina);

        assertSame(pagina, useCase.ejecutar(titular, null, SolicitudPagina.primera()));
    }

    @Test
    void agregaElFiltroDeEstadoSinSoltarElDocumento() {
        FiltroReservas esperado = new FiltroReservas(null, null, EstadoReserva.PENDIENTE, null, null, titular);
        Pagina<Reserva> pagina = new Pagina<>(List.of(), 1, 0);
        when(reservaRepository.buscar(esperado, new SolicitudPagina(1))).thenReturn(pagina);

        assertSame(pagina, useCase.ejecutar(titular, EstadoReserva.PENDIENTE, new SolicitudPagina(1)));
        verify(reservaRepository).buscar(esperado, new SolicitudPagina(1));
    }
}
