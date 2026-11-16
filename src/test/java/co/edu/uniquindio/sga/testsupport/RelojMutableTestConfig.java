package co.edu.uniquindio.sga.testsupport;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Registra un {@link RelojMutable} como reloj principal de la aplicación, empezando en el instante de la propiedad
 * {@code test.reloj-inicial} (por ejemplo {@code 2027-03-10T10:00}, en la zona horaria del alojamiento). El datos de
 * prueba se siembran con ese "ahora". Uso: {@code @Import(RelojMutableTestConfig.class)} y
 * {@code @Autowired RelojMutable reloj} en la prueba.
 */
@TestConfiguration
public class RelojMutableTestConfig {

    @Bean
    @Primary
    public RelojMutable relojMutableDePrueba(@Value("${test.reloj-inicial}") String inicial,
                                             @Value("${sga.alojamiento.zona-horaria}") String zona) {
        return new RelojMutable(ZoneId.of(zona), LocalDateTime.parse(inicial));
    }
}
