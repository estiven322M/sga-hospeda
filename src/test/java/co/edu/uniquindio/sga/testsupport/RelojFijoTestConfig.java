package co.edu.uniquindio.sga.testsupport;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Reemplaza el reloj del alojamiento por uno fijo, con el instante de la propiedad {@code test.reloj-fijo}
 * (por ejemplo {@code 2027-06-05T12:00}) interpretado en la zona horaria del alojamiento. Todo lo que recibe un
 * {@code Clock} (datos sembrados, controllers, vencimiento programado) ve ese "ahora", así que las pruebas pueden
 * ejercitar fechas de cualquier temporada sin depender del día en que se ejecutan.
 *
 * Uso: {@code @SpringBootTest(properties = "test.reloj-fijo=2027-06-05T12:00") @Import(RelojFijoTestConfig.class)}.
 */
@TestConfiguration
public class RelojFijoTestConfig {

    @Bean
    @Primary
    public Clock relojFijoDePrueba(@Value("${test.reloj-fijo}") String instante,
                                   @Value("${sga.alojamiento.zona-horaria}") String zona) {
        ZoneId zonaHoraria = ZoneId.of(zona);
        return Clock.fixed(LocalDateTime.parse(instante).atZone(zonaHoraria).toInstant(), zonaHoraria);
    }
}
