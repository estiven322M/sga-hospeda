package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;
import co.edu.uniquindio.sga.domain.valueobject.ReservasDeCanal;
import co.edu.uniquindio.sga.testsupport.ReservasParaPruebas;

/** Fórmulas de los reportes (7.9, L-19): cada una con su caso normal, el periodo sin datos y el rango inválido. */
class ReportesServiceTest {

    private static final LocalDate DIA_10 = LocalDate.of(2026, 11, 10);
    private static final Periodo NOVIEMBRE_10_AL_19 = new Periodo(DIA_10, LocalDate.of(2026, 11, 19));

    private final ReportesService reportes = new ReportesService();

    private static Reserva confirmada(String codigo, LocalDate entrada, LocalDate salida) {
        return ReservasParaPruebas.confirmada(codigo, "APT-101", entrada, salida, 1);
    }

    private static Reserva pendiente(String codigo, LocalDate entrada, LocalDate salida) {
        return ReservasParaPruebas.pendiente(codigo, "APT-101", entrada, salida, 1);
    }

    private static Reserva cancelada(String codigo) {
        Reserva reserva = pendiente(codigo, DIA_10, DIA_10.plusDays(2));
        reserva.cancelar("Cambio de planes", "Recepción", ReservasParaPruebas.AHORA.plusHours(2));
        return reserva;
    }

    // ---------- rango de fechas ----------

    @Test
    void unRangoConElFinAnteriorAlInicioViolaLaRegla() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> new Periodo(LocalDate.of(2026, 11, 19), DIA_10));
        assertEquals("El fin del periodo no puede ser anterior a su inicio", error.getMessage());
    }

    @Test
    void unRangoSinFechasViolaLaRegla() {
        assertThrows(ReglaDominioException.class, () -> new Periodo(null, DIA_10));
    }

    // ---------- ocupación ----------

    @Test
    void ocupacion_esNochesVendidasSobreApartamentosActivosPorNochesDelPeriodo() {
        // 10 noches de periodo × 4 apartamentos = 40 disponibles; 3 + 2 = 5 vendidas → 12,50 %
        List<Reserva> reservas = List.of(
                confirmada("RES-2026-00001", DIA_10, DIA_10.plusDays(3)),
                confirmada("RES-2026-00002", DIA_10.plusDays(5), DIA_10.plusDays(7)));

        ReporteOcupacion reporte = reportes.ocupacion(NOVIEMBRE_10_AL_19, reservas, 4);

        assertEquals(10, reporte.nochesPeriodo());
        assertEquals(40, reporte.nochesDisponibles());
        assertEquals(5, reporte.nochesVendidas());
        assertEquals(new BigDecimal("12.50"), reporte.porcentaje());
    }

    @Test
    void ocupacion_soloCuentaLasNochesQueCaenDentroDelPeriodo() {
        // estancia del 8 al 12: solo las noches del 10 y del 11 están en el periodo;
        // estancia del 18 al 25: solo las noches del 18 y del 19 (el periodo incluye su último día)
        List<Reserva> reservas = List.of(
                confirmada("RES-2026-00001", LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 12)),
                confirmada("RES-2026-00002", LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 25)),
                confirmada("RES-2026-00003", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3)));

        assertEquals(4, reportes.ocupacion(NOVIEMBRE_10_AL_19, reservas, 1).nochesVendidas());
    }

    @Test
    void ocupacion_lasPendientesYLasCanceladasNoVendenNoches() {
        List<Reserva> reservas = List.of(pendiente("RES-2026-00001", DIA_10, DIA_10.plusDays(3)),
                cancelada("RES-2026-00002"));

        ReporteOcupacion reporte = reportes.ocupacion(NOVIEMBRE_10_AL_19, reservas, 2);

        assertEquals(0, reporte.nochesVendidas());
        assertEquals(new BigDecimal("0.00"), reporte.porcentaje());
    }

    @Test
    void ocupacion_elPorcentajeSeRedondeaADosDecimalesMitadHaciaArriba() {
        // 1 noche vendida de 3 disponibles = 33,333… → 33,33; 2 de 3 = 66,666… → 66,67
        Periodo unaNoche = new Periodo(DIA_10, DIA_10);
        Reserva una = confirmada("RES-2026-00001", DIA_10, DIA_10.plusDays(1));
        Reserva otra = confirmada("RES-2026-00002", DIA_10, DIA_10.plusDays(1));

        assertEquals(new BigDecimal("33.33"), reportes.ocupacion(unaNoche, List.of(una), 3).porcentaje());
        assertEquals(new BigDecimal("66.67"), reportes.ocupacion(unaNoche, List.of(una, otra), 3).porcentaje());
    }

    @Test
    void ocupacion_unPeriodoSinDatosDaCeroYNoDivideEntreCero() {
        ReporteOcupacion sinReservas = reportes.ocupacion(NOVIEMBRE_10_AL_19, List.of(), 4);
        assertEquals(0, sinReservas.nochesVendidas());
        assertEquals(new BigDecimal("0.00"), sinReservas.porcentaje());

        ReporteOcupacion sinApartamentos = reportes.ocupacion(NOVIEMBRE_10_AL_19, List.of(), 0);
        assertEquals(0, sinApartamentos.nochesDisponibles());
        assertEquals(new BigDecimal("0.00"), sinApartamentos.porcentaje());
    }

    @Test
    void ocupacion_unaCantidadNegativaDeApartamentosViolaLaRegla() {
        assertThrows(ReglaDominioException.class, () -> reportes.ocupacion(NOVIEMBRE_10_AL_19, List.of(), -1));
    }

    // ---------- ingresos ----------

    @Test
    void ingresos_sumaLosPagosDelPeriodoPorMedioDePago() {
        List<Pago> pagos = List.of(
                new Pago("EFECTIVO", DIA_10, Dinero.de(100_000)),
                new Pago("TARJETA", DIA_10.plusDays(1), Dinero.de(250_000)),
                new Pago("efectivo", LocalDate.of(2026, 11, 19), Dinero.de(50_000)),
                new Pago("EFECTIVO", LocalDate.of(2026, 11, 9), Dinero.de(999_000)),     // antes del periodo
                new Pago("TARJETA", LocalDate.of(2026, 11, 20), Dinero.de(999_000)));    // después del periodo

        ReporteIngresos reporte = reportes.ingresos(NOVIEMBRE_10_AL_19, pagos);

        assertEquals(2, reporte.porMedio().size());
        assertEquals("EFECTIVO", reporte.porMedio().get(0).medio());
        assertEquals(Dinero.de(150_000), reporte.porMedio().get(0).total());
        assertEquals("TARJETA", reporte.porMedio().get(1).medio());
        assertEquals(Dinero.de(250_000), reporte.porMedio().get(1).total());
        assertEquals(Dinero.de(400_000), reporte.total());
    }

    @Test
    void ingresos_unPeriodoSinPagosDaCero() {
        ReporteIngresos reporte = reportes.ingresos(NOVIEMBRE_10_AL_19, List.of());

        assertTrue(reporte.porMedio().isEmpty());
        assertTrue(reporte.total().esCero());
    }

    // ---------- reservas por canal ----------

    @Test
    void reservasPorCanal_cuentaTodasLasReservasCreadasEnElPeriodoIncluidasLasCanceladas() {
        // las reservas de prueba se crean el 2-nov-2026 por el canal DIRECTO
        Periodo conLaCreacion = new Periodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        List<Reserva> reservas = List.of(pendiente("RES-2026-00001", DIA_10, DIA_10.plusDays(2)),
                confirmada("RES-2026-00002", DIA_10, DIA_10.plusDays(2)), cancelada("RES-2026-00003"));
        assertEquals(EstadoReserva.CANCELADA, reservas.get(2).getEstado());

        ReporteReservasPorCanal reporte = reportes.reservasPorCanal(conLaCreacion, reservas);

        assertEquals(CanalOrigen.values().length, reporte.canales().size());
        ReservasDeCanal directo = reporte.canales().stream()
                .filter(fila -> fila.canal() == CanalOrigen.DIRECTO).findFirst().orElseThrow();
        assertEquals(new ReservasDeCanal(CanalOrigen.DIRECTO, 3, 1, 0), directo);
        assertEquals(3, reporte.total());
        assertEquals(1, reporte.canceladas());
        assertEquals(0, reporte.noShow());
    }

    @Test
    void reservasPorCanal_unPeriodoSinReservasDaTodosLosCanalesEnCero() {
        // ninguna reserva de prueba se creó entre el 10 y el 19 de noviembre
        List<Reserva> reservas = List.of(pendiente("RES-2026-00001", DIA_10, DIA_10.plusDays(2)));

        ReporteReservasPorCanal reporte = reportes.reservasPorCanal(NOVIEMBRE_10_AL_19, reservas);

        assertEquals(0, reporte.total());
        assertTrue(reporte.canales().stream().allMatch(fila -> fila.total() == 0 && fila.canceladas() == 0));
    }
}
