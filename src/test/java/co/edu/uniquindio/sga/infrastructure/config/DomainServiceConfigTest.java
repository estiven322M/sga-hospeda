package co.edu.uniquindio.sga.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;

/** Los parámetros de la Ficha llegan desde application.properties; el puerto del dominio los expone tal cual. */
class DomainServiceConfigTest {

    private final DomainServiceConfig config = new DomainServiceConfig();

    private ConfiguracionAlojamiento configuracion(int tiempoPreparacion, int umbral) {
        return configuracion(tiempoPreparacion, umbral, 7, 10, 18, entornoConMinimos());
    }

    private static MockEnvironment entornoConMinimos() {
        return new MockEnvironment()
                .withProperty("sga.alojamiento.estancia-minima.ALTA", "3")
                .withProperty("sga.alojamiento.estancia-minima.MEDIA", "2");
    }

    private ConfiguracionAlojamiento configuracion(int tiempoPreparacion, int umbral, int nochesDescuento,
                                                   int porcentajeDescuento, int edadAdulto, MockEnvironment entorno) {
        return config.configuracionAlojamiento("America/Bogota", "15:00", "11:00",
                tiempoPreparacion, 24, umbral, "22:00", 30, nochesDescuento, porcentajeDescuento, edadAdulto, entorno);
    }

    @Test
    void exponeLosParametrosDeLaFicha() {
        ConfiguracionAlojamiento c = configuracion(3, 12);

        assertEquals(3, c.tiempoPreparacion().horas());
        assertEquals(24, c.plazoConfirmacion().horas());
        assertEquals(12, c.umbralEdadFacturable().anios());
        assertEquals(LocalTime.of(15, 0), c.horaEntrada());
        assertEquals(LocalTime.of(11, 0), c.horaSalida());
        assertEquals(LocalTime.of(22, 0), c.horaLimiteNoShow());
        assertEquals(ZoneId.of("America/Bogota"), c.zonaHoraria());
        assertEquals(30, c.anticipoPorcentaje());
    }

    @Test
    void exponeLasReglasPropiasDeL20() {
        ConfiguracionAlojamiento c = configuracion(3, 12);

        assertEquals(Map.of("ALTA", 3, "MEDIA", 2), c.estanciaMinimaPorTemporada()); // RP-01
        assertEquals(7, c.nochesDescuentoEstanciaLarga());                           // RP-02
        assertEquals(10, c.porcentajeDescuentoEstanciaLarga());
        assertEquals(18, c.edadAdultoResponsableAnios());                            // RP-03
    }

    @Test
    void sinEstanciasMinimasConfiguradasNingunaTemporadaExigeMinimo() {
        ConfiguracionAlojamiento c = configuracion(3, 12, 7, 10, 18, new MockEnvironment());

        assertTrue(c.estanciaMinimaPorTemporada().isEmpty());
    }

    @Test
    void unaReglaPropiaMalConfiguradaImpideArrancar() {
        // 150 % de descuento, edad de adulto de 99 años y una estancia mínima de 0 noches no tienen sentido
        assertThrows(ReglaDominioException.class, () -> configuracion(3, 12, 7, 150, 18, entornoConMinimos()));
        assertThrows(ReglaDominioException.class, () -> configuracion(3, 12, 7, 10, 99, entornoConMinimos()));
        assertThrows(ReglaDominioException.class, () -> configuracion(3, 12, 7, 10, 18,
                new MockEnvironment().withProperty("sga.alojamiento.estancia-minima.ALTA", "0")));
    }

    @Test
    void elRelojUsaLaZonaHorariaConfigurada() {
        Clock reloj = config.reloj("America/Bogota");
        assertEquals(ZoneId.of("America/Bogota"), reloj.getZone());
    }

    @Test
    void unParametroInvalidoEsRechazadoPorElObjetoDeValor() {
        assertThrows(ReglaDominioException.class, () -> configuracion(-1, 12));
        assertThrows(ReglaDominioException.class, () -> configuracion(3, 99));
    }
}
