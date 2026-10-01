package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReservaTest {
	
	// --- MÉTODOS ARRANGE (Preparación del estado inicial) ---

    private Ocupante titular() {
        return new Ocupante(new DocumentoIdentidad("CC-1000123456"), "Ana Gómez",
                LocalDate.of(1990, 5, 20), LocalDate.of(2026, 1, 1));
    }

    private ValorCongelado valorBase() {
        // Simulamos el costo de 2 noches
        return new ValorCongelado(List.of(
            new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100000), 1),
            new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100000), 1)
        ));
    }

    private Reserva reservaBase() {
        return Reserva.crear(
                new CodigoReserva("RES-2026-00001"),
                new IdentificacionApartamento("APT-301"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                titular(),
                List.of(titular()),
                CanalOrigen.PORTAL,
                valorBase(),
                new VersionPolitica(1),
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
    }

    // --- PRUEBAS (Act & Assert) ---

    @Test
    void debeNacerEnEstadoPendiente() {
        Reserva reserva = reservaBase();
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void noDebeConfirmarSinHoraEstimadaDeLlegadaRN09() {
        Reserva reserva = reservaBase();
        LocalDateTime ahora = LocalDateTime.of(2026, 1, 2, 10, 0);

        // Intentamos confirmar directamente
        ReglaDominioException ex = assertThrows(ReglaDominioException.class, 
            () -> reserva.confirmar("Recepcionista", ahora));

        assertEquals("No se puede confirmar una reserva sin hora estimada de llegada", ex.getMessage());
        
        // ASERCIÓN DE ESTADO INTACTO: La reserva se defendió y sigue PENDIENTE
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void debeConfirmarCorrectamenteConHoraDeLlegada() {
        Reserva reserva = reservaBase();
        LocalDateTime ahora = LocalDateTime.of(2026, 1, 2, 10, 0);
        
        // Cumplimos la precondición (RN-09)
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(14, 0), "Titular", ahora);
        reserva.confirmar("Recepcionista", ahora);

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    void noDebePermitirModificarListaDeOcupantesDesdeAfuera() {
        Reserva reserva = reservaBase();
        Ocupante intruso = new Ocupante(new DocumentoIdentidad("CC-999"), "Intruso", 
                                        LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));
        
        // La lista debe ser inmutable hacia el exterior (List.copyOf)
        assertThrows(UnsupportedOperationException.class, () -> {
            reserva.getOcupantes().add(intruso);
        });
    }

}
