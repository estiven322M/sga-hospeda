package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-19: canal + identificador externo es único; solo los canales externos llevan identificador. */
class RecepcionReservaExternaServiceTest {

    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final RecepcionReservaExternaService servicio = new RecepcionReservaExternaService(reservaRepository);
    private final IdentificadorExterno mensaje = new IdentificadorExterno("BKG-778899");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private Reserva reservaExterna() {
        return Reserva.crear(new CodigoReserva("RES-2026-00007"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11)), titular, List.of(titular),
                CanalOrigen.EXTERNO, mensaje,
                new ValorCongelado(List.of(new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void aceptaUnMensajeExternoQueNoSeHabiaRecibido() {
        when(reservaRepository.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, mensaje))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> servicio.verificarRecepcion(CanalOrigen.EXTERNO, mensaje));
    }

    @Test
    void rechazaElMismoMensajeRecibidoDosVecesYDiceQueReservaLoCreo() {
        when(reservaRepository.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, mensaje))
                .thenReturn(Optional.of(reservaExterna()));

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> servicio.verificarRecepcion(CanalOrigen.EXTERNO, mensaje));

        assertTrue(error.getMessage().contains("BKG-778899"));
        assertTrue(error.getMessage().contains("RES-2026-00007"));
    }

    @Test
    void unCanalExternoSinIdentificadorSeRechazaSinConsultarNada() {
        assertThrows(ReglaDominioException.class, () -> servicio.verificarRecepcion(CanalOrigen.EXTERNO, null));
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void losCanalesQueNoSonExternosNoLlevanIdentificador() {
        assertThrows(ReglaDominioException.class, () -> servicio.verificarRecepcion(CanalOrigen.PORTAL, mensaje));
        assertThrows(ReglaDominioException.class, () -> servicio.verificarRecepcion(CanalOrigen.DIRECTO, mensaje));
        assertDoesNotThrow(() -> servicio.verificarRecepcion(CanalOrigen.PORTAL, null));
        assertDoesNotThrow(() -> servicio.verificarRecepcion(CanalOrigen.DIRECTO, null));
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void elCanalEsObligatorio() {
        assertThrows(ReglaDominioException.class, () -> servicio.verificarRecepcion(null, null));
    }
}
