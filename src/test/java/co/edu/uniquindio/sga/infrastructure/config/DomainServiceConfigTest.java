package co.edu.uniquindio.sga.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;

/** Los parámetros de la Ficha llegan desde application.properties; el puerto del dominio los expone tal cual. */
class DomainServiceConfigTest {

    private final DomainServiceConfig config = new DomainServiceConfig();

    private ConfiguracionAlojamiento configuracion(int tiempoPreparacion, int umbral) {
        return config.configuracionAlojamiento("America/Bogota", "15:00", "11:00",
                tiempoPreparacion, 24, umbral, "22:00", 30);
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
