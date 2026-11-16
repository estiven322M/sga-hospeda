package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.testsupport.ReservasParaPruebas;

/**
 * L-17, 7.7: el cargo de un servicio adicional se registra solo con el folio abierto, el servicio activo (y que genere
 * cargo) y la reserva CONFIRMADA o EN_CURSO. Cada regla se prueba también en el caso en que se viola.
 */
class CargoServicioAdicionalServiceTest {

    private final CargoServicioAdicionalService servicio = new CargoServicioAdicionalService();
    private Reserva confirmada;
    private Folio folio;
    private ServicioAdicional desayuno;

    @BeforeEach
    void setUp() {
        confirmada = ReservasParaPruebas.confirmada("RES-2026-00001", "APT-101", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 2);
        folio = new Folio(confirmada.getCodigo(), new Cargo("Alojamiento", Dinero.de(400_000)));
        desayuno = new ServicioAdicional("DESAYUNO", "Desayuno típico", "", true, Dinero.de(25_000));
    }

    @Test
    void registraElCargoConElValorDelServicioPorLaCantidadEnUnaReservaConfirmada() {
        Cargo cargo = servicio.registrar(confirmada, folio, desayuno, 3);

        assertEquals(Dinero.de(75_000), cargo.valor());
        assertEquals("Servicio adicional: Desayuno típico x3", cargo.concepto());
        assertEquals(2, folio.obtenerCargos().size());
        assertEquals(Dinero.de(475_000), folio.saldo());
    }

    @Test
    void tambienSeRegistraEnUnaReservaEnCurso() {
        Reserva enCurso = ReservasParaPruebas.confirmada("RES-2026-00002", "APT-102", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), 1);
        enCurso.registrarLlegada("Recepción", ReservasParaPruebas.AHORA.plusHours(6));
        Folio folioEnCurso = new Folio(enCurso.getCodigo(), new Cargo("Alojamiento", Dinero.de(200_000)));

        servicio.registrar(enCurso, folioEnCurso, desayuno, 1);

        assertEquals(Dinero.de(225_000), folioEnCurso.saldo());
    }

    @Test
    void rechazaUnaReservaPendienteConSuMensaje() {
        Reserva pendiente = ReservasParaPruebas.pendiente("RES-2026-00003", "APT-101", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 1);
        Folio folioPendiente = new Folio(pendiente.getCodigo(), new Cargo("Alojamiento", Dinero.de(200_000)));

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> servicio.registrar(pendiente, folioPendiente, desayuno, 1));

        assertEquals("Solo se registran servicios adicionales en reservas CONFIRMADA o EN_CURSO; la reserva RES-2026-00003 está PENDIENTE", e.getMessage());
        assertEquals(1, folioPendiente.obtenerCargos().size()); // no se registró nada
    }

    @Test
    void rechazaUnaReservaCanceladaOFinalizada() {
        Reserva cancelada = ReservasParaPruebas.pendiente("RES-2026-00004", "APT-101", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 1);
        cancelada.cancelar("Cambio de planes", "Recepción", ReservasParaPruebas.AHORA.plusHours(1));
        Folio folioCancelada = new Folio(cancelada.getCodigo(), new Cargo("Alojamiento", Dinero.de(200_000)));

        assertThrows(ReglaDominioException.class, () -> servicio.registrar(cancelada, folioCancelada, desayuno, 1));
    }

    @Test
    void rechazaUnServicioDadoDeBaja() {
        desayuno.darDeBaja();

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> servicio.registrar(confirmada, folio, desayuno, 1));

        assertEquals("El servicio adicional DESAYUNO está dado de baja", e.getMessage());
        assertEquals(1, folio.obtenerCargos().size());
    }

    @Test
    void rechazaUnServicioQueNoGeneraCargo() {
        ServicioAdicional cortesia = new ServicioAdicional("BIENVENIDA", "Detalle de bienvenida", "", false, Dinero.CERO);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> servicio.registrar(confirmada, folio, cortesia, 1));

        assertEquals("El servicio adicional BIENVENIDA no genera cargo", e.getMessage());
    }

    @Test
    void rechazaUnFolioCerrado() {
        folio.registrarPago(new co.edu.uniquindio.sga.domain.valueobject.Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(400_000)));
        folio.cerrar(null);

        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> servicio.registrar(confirmada, folio, desayuno, 1));

        assertEquals("Un folio cerrado no admite movimientos", e.getMessage());
    }

    @Test
    void rechazaUnaCantidadMenorQueUnoYUnFolioDeOtraReserva() {
        assertThrows(ReglaDominioException.class, () -> servicio.registrar(confirmada, folio, desayuno, 0));
        assertThrows(ReglaDominioException.class, () -> servicio.registrar(confirmada, folio, desayuno, -2));

        Folio ajeno = new Folio(new co.edu.uniquindio.sga.domain.valueobject.CodigoReserva("RES-2026-09999"),
                new Cargo("Alojamiento", Dinero.de(1)));
        assertThrows(ReglaDominioException.class, () -> servicio.registrar(confirmada, ajeno, desayuno, 1));
    }
}
