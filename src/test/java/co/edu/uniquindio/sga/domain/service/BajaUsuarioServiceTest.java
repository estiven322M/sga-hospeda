package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/** 7.2: no se desactiva a un usuario que sea titular de reservas PENDIENTE, CONFIRMADA o EN_CURSO. */
class BajaUsuarioServiceTest {

    private static final DocumentoIdentidad TITULAR = new DocumentoIdentidad("1094000111");

    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final BajaUsuarioService servicio = new BajaUsuarioService(reservas);

    private void conReservasEn(EstadoReserva estado, long cuantas) {
        when(reservas.buscar(argThat(f -> f != null && f.estado() == estado && TITULAR.equals(f.titular())), any(SolicitudPagina.class)))
                .thenReturn(new Pagina<>(List.of(), 0, cuantas));
    }

    private void sinReservas() {
        when(reservas.buscar(any(FiltroReservas.class), any(SolicitudPagina.class))).thenReturn(new Pagina<>(List.of(), 0, 0));
    }

    @Test
    void sePuedeDesactivarAUnTitularSinReservasActivas() {
        sinReservas();

        assertDoesNotThrow(() -> servicio.verificarPuedeDesactivarse(TITULAR));
    }

    @Test
    void noSeDesactivaAUnTitularConUnaReservaPendiente() {
        sinReservas();
        conReservasEn(EstadoReserva.PENDIENTE, 1);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> servicio.verificarPuedeDesactivarse(TITULAR));

        assertTrue(e.getMessage().contains("PENDIENTE"));
        assertTrue(e.getMessage().contains("No se puede desactivar al usuario"));
    }

    @Test
    void noSeDesactivaAUnTitularConUnaReservaConfirmada() {
        sinReservas();
        conReservasEn(EstadoReserva.CONFIRMADA, 2);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> servicio.verificarPuedeDesactivarse(TITULAR));

        assertTrue(e.getMessage().contains("CONFIRMADA (2)"));
    }

    @Test
    void noSeDesactivaAUnTitularConUnaReservaEnCurso() {
        sinReservas();
        conReservasEn(EstadoReserva.EN_CURSO, 1);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> servicio.verificarPuedeDesactivarse(TITULAR));

        assertTrue(e.getMessage().contains("EN_CURSO"));
    }

    @Test
    void lasReservasHistoricasNoImpidenLaBaja() {
        sinReservas();
        conReservasEn(EstadoReserva.FINALIZADA, 4);
        conReservasEn(EstadoReserva.CANCELADA, 2);
        conReservasEn(EstadoReserva.NO_SHOW, 1);

        assertDoesNotThrow(() -> servicio.verificarPuedeDesactivarse(TITULAR));
    }
}
