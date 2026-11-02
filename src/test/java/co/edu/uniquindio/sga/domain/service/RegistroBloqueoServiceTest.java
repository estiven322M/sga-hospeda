package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** 7.3: no se bloquean noches que ya tienen reservas activas. */
class RegistroBloqueoServiceTest {

    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final RegistroBloqueoService servicio = new RegistroBloqueoService(reservaRepository);
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private Reserva reservaDel10al12() {
        return Reserva.crear(new CodigoReserva("RES-2026-00001"), apt,
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void creaElBloqueoSiNoHayReservasActivasEnEsasNoches() {
        when(reservaRepository.buscarActivasPorApartamento(apt)).thenReturn(List.of(reservaDel10al12()));

        // Empieza el 12, que es la fecha de salida de la reserva: no comparten ninguna noche
        Bloqueo bloqueo = servicio.crear(apt,
                new Estancia(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15)), "Mantenimiento");

        assertEquals("Mantenimiento", bloqueo.getMotivo());
        assertEquals(apt, bloqueo.getApartamento());
    }

    @Test
    void rechazaBloquearNochesConReservaActiva() {
        when(reservaRepository.buscarActivasPorApartamento(apt)).thenReturn(List.of(reservaDel10al12()));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> servicio.crear(apt,
                new Estancia(LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 14)), "Mantenimiento"));

        assertTrue(ex.getMessage().contains("RES-2026-00001"));
    }

    @Test
    void elBloqueoExigeMotivoSinConsultarReservas() {
        assertThrows(ReglaDominioException.class, () -> servicio.crear(apt,
                new Estancia(LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 14)), " "));

        verifyNoInteractions(reservaRepository);
    }
}
