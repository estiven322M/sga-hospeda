package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-13: la retención sale de la política congelada en la reserva; la antelación se mide hasta la hora de entrada. */
class RetencionCancelacionServiceTest {

    private final ConfiguracionAlojamiento config = mock(ConfiguracionAlojamiento.class);
    private final RetencionCancelacionService servicio = new RetencionCancelacionService(config);

    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    // Entrada el 10-dic-2026 (15:00); valor total 200.000; política congelada versión 2
    private Reserva reserva() {
        return Reserva.crear(new CodigoReserva("RES-2026-00001"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL,
                new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(2), LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    private PoliticaCancelacion politica(int version) {
        // 72 h o más: 0 %; de 24 a 72 h: 50 %; menos de 24 h: 100 %
        return new PoliticaCancelacion(new VersionPolitica(version), LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(72, 0), new TramoRetencion(24, 50), new TramoRetencion(0, 100)));
    }

    private RetencionCancelacionService conEntrada15() {
        when(config.horaEntrada()).thenReturn(LocalTime.of(15, 0));
        return servicio;
    }

    @Test
    void conAntelacionSuficienteNoRetiene() {
        // 7-dic 15:00 -> 10-dic 15:00 = 72 h exactas
        assertEquals(Dinero.CERO, conEntrada15().retencionPorCancelacion(reserva(), politica(2),
                LocalDateTime.of(2026, 12, 7, 15, 0)));
    }

    @Test
    void laAntelacionSeMideHastaLaHoraDeEntradaNoHastaLaMedianoche() {
        // 7-dic 15:01 -> 71 h 59 min: cae en el tramo del 50 %
        assertEquals(Dinero.de(100_000), conEntrada15().retencionPorCancelacion(reserva(), politica(2),
                LocalDateTime.of(2026, 12, 7, 15, 1)));
    }

    @Test
    void cancelarElMismoDiaDeLaEntradaRetieneTodo() {
        assertEquals(Dinero.de(200_000), conEntrada15().retencionPorCancelacion(reserva(), politica(2),
                LocalDateTime.of(2026, 12, 10, 9, 0)));
    }

    @Test
    void rechazaCalcularConUnaPoliticaDistintaALaCongelada() {
        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> servicio.retencionPorCancelacion(reserva(), politica(3), LocalDateTime.of(2026, 11, 3, 9, 0)));
        assertTrue(ex.getMessage().contains("versión 2"));
    }
}
