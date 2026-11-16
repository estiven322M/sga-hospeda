package co.edu.uniquindio.sga.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EdadAdultoResponsable;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/**
 * Los datos de prueba se siembran con fechas relativas a "hoy" (SembradorReservasPrueba). Desde que existen reglas
 * que dependen de la temporada (RP-01: estancia mínima), un sembrado que fuera válido un día podría dejar de serlo en
 * otro y la aplicación no arrancaría. Cada subclase arranca la aplicación COMPLETA con un reloj fijo en una fecha
 * cuyas reservas sembradas caen en temporada ALTA: si el sembrado lanzara una sola excepción, el contexto de Spring
 * no se levantaría y la prueba fallaría.
 *
 * Cada subclase usa su PROPIA base de datos en memoria (propiedad {@code spring.datasource.url}): con la URL por defecto
 * todos los contextos de Spring de una misma ejecución comparten la base, y un contexto que encuentra los datos ya
 * sembrados por otro no siembra los suyos, con lo que la prueba comprobaría otra cosa.
 */
@Transactional
abstract class SembradoConRelojFijoBase {

    private static final FiltroReservas SIN_FILTRO = new FiltroReservas(null, null, null, null, null, null);
    private static final EdadAdultoResponsable ADULTO = new EdadAdultoResponsable(18);

    @Autowired private ReservaRepository reservas;
    @Autowired private Clock reloj;

    /** Fecha que fija el reloj de la prueba. */
    abstract LocalDate hoyEsperado();

    private List<Reserva> sembradas() {
        return reservas.buscar(SIN_FILTRO, SolicitudPagina.primera()).contenido();
    }

    @Test
    void laAplicacionArrancaConElRelojFijoYElSembradoUsaEsaFecha() {
        assertEquals(hoyEsperado(), LocalDate.now(reloj));
        assertEquals(6, sembradas().size(), "una reserva por cada estado");
    }

    @Test
    void elSembradoCreaUnaReservaEnCadaEstadoYUnaPorCadaCanal() {
        assertEquals(Set.of(EstadoReserva.values()),
                sembradas().stream().map(Reserva::getEstado).collect(Collectors.toSet()));
        assertEquals(Set.of(CanalOrigen.values()),
                sembradas().stream().map(Reserva::getCanalOrigen).collect(Collectors.toSet()));
    }

    @Test
    void almenosUnaReservaSembradaCaeEnTemporadaAltaPorqueDeOtroModoLaPruebaNoDemostrariaNada() {
        assertTrue(sembradas().stream().flatMap(r -> r.getValor().detalle().stream())
                        .anyMatch(cargo -> cargo.temporada().equals("ALTA")),
                "el reloj fijo debe hacer que el sembrado toque la temporada ALTA");
    }

    @Test
    void todaEstanciaSembradaDuraAlMenosTresNochesElMayorMinimoQueExigeUnaTemporada() {
        for (Reserva reserva : sembradas()) {
            assertTrue(reserva.getEstancia().noches() >= SembradorReservasPrueba.NOCHES,
                    reserva.getCodigo().valor() + " dura " + reserva.getEstancia().noches() + " noches");
            assertEquals(SembradorReservasPrueba.NOCHES, reserva.getValor().noches());
        }
    }

    @Test
    void todaReservaSembradaIncluyeAlMenosUnAdultoResponsableRP03() {
        for (Reserva reserva : sembradas()) {
            assertTrue(reserva.getOcupantes().stream().anyMatch(o -> o.esAdultoEn(reserva.getEstancia(), ADULTO)),
                    reserva.getCodigo().valor() + " no tiene adulto responsable");
        }
    }

    @Test
    void ningunaReservaSembradaTieneDescuentoPorqueNingunaLlegaASieteNoches() {
        assertFalse(sembradas().stream().anyMatch(r -> r.getValor().porcentajeDescuento() > 0));
    }
}
