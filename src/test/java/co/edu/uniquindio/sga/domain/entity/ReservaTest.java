package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.testsupport.ReservaDePrueba;

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
                LocalDateTime.of(2026, 1, 1, 10, 0), ReservaDePrueba.EDAD_ADULTO
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

    // ---------- RP-03 (L-20): adulto responsable al crear ----------

    private Reserva crearConGrupo(Ocupante titular, List<Ocupante> ocupantes, EdadAdultoResponsable edad) {
        return Reserva.crear(new CodigoReserva("RES-2026-00009"), new IdentificacionApartamento("APT-301"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, ocupantes,
                CanalOrigen.DIRECTO, valorBase(), new VersionPolitica(1), LocalDateTime.of(2026, 1, 1, 10, 0), edad);
    }

    private Ocupante nacidoEl(String documento, LocalDate nacimiento) {
        return new Ocupante(new DocumentoIdentidad(documento), "Persona " + documento, nacimiento,
                LocalDate.of(2026, 1, 1));
    }

    private static final String MENSAJE_RP03 = "La reserva debe incluir al menos un ocupante mayor de 18 años";

    @Test
    void unaReservaSoloConMenoresSeRechazaRP03() {
        Ocupante menor = nacidoEl("TI-1", LocalDate.of(2011, 6, 1)); // 15 años

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> crearConGrupo(menor, List.of(menor), ReservaDePrueba.EDAD_ADULTO));

        assertEquals(MENSAJE_RP03, ex.getMessage());
    }

    @Test
    void conDiecisieteAniosYTrescientosSesentaYCuatroDiasLaReservaSeRechazaRP03() {
        Ocupante casiAdulto = nacidoEl("TI-2", LocalDate.of(2008, 12, 11)); // cumple 18 un día después de la entrada

        assertThrows(ReglaDominioException.class,
                () -> crearConGrupo(casiAdulto, List.of(casiAdulto), ReservaDePrueba.EDAD_ADULTO));
    }

    @Test
    void conExactamenteDieciochoAniosALaEntradaLaReservaSePermiteRP03() {
        Ocupante recienAdulto = nacidoEl("CC-3", LocalDate.of(2008, 12, 10));

        Reserva reserva = crearConGrupo(recienAdulto, List.of(recienAdulto), ReservaDePrueba.EDAD_ADULTO);

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void unTitularMenorPuedeReservarSiAlguienDelGrupoEsAdultoRP03() {
        Ocupante menor = nacidoEl("TI-4", LocalDate.of(2011, 6, 1));
        Ocupante adulto = nacidoEl("CC-5", LocalDate.of(1985, 3, 3));

        Reserva reserva = crearConGrupo(menor, List.of(menor, adulto), ReservaDePrueba.EDAD_ADULTO);

        assertEquals(menor, reserva.getTitular());
    }

    @Test
    void basta_un_solo_adulto_entre_varios_menores_RP03() {
        Ocupante titular = nacidoEl("CC-6", LocalDate.of(1990, 1, 1));
        Ocupante hijo1 = nacidoEl("TI-7", LocalDate.of(2014, 1, 1));
        Ocupante hijo2 = nacidoEl("TI-8", LocalDate.of(2016, 1, 1));

        assertEquals(3, crearConGrupo(titular, List.of(titular, hijo1, hijo2), ReservaDePrueba.EDAD_ADULTO)
                .getOcupantes().size());
    }

    @Test
    void laEdadDelAdultoResponsableEsUnParametroDeLaCreacionRP03() {
        Ocupante veinte = nacidoEl("CC-9", LocalDate.of(2006, 6, 1));

        assertEquals(EstadoReserva.PENDIENTE,
                crearConGrupo(veinte, List.of(veinte), new EdadAdultoResponsable(18)).getEstado());
        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> crearConGrupo(veinte, List.of(veinte), new EdadAdultoResponsable(21)));
        assertEquals("La reserva debe incluir al menos un ocupante mayor de 21 años", ex.getMessage());
    }

    @Test
    void crearSinConocerLaEdadDelAdultoResponsableSeRechazaRP03() {
        assertThrows(ReglaDominioException.class, () -> crearConGrupo(titular(), List.of(titular()), null));
    }
}
