package co.edu.uniquindio.sga.infrastructure.config;

import java.time.LocalDate;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.sga.testsupport.RelojFijoTestConfig;

/**
 * Arranque el 10 de diciembre de 2027: las reservas sembradas a +10 y +20 días caen en la ALTA de fin de año
 * (15-dic a 15-ene, que cruza el cambio de año).
 */
@SpringBootTest(properties = {"test.reloj-fijo=2027-12-10T12:00", "spring.datasource.url=jdbc:h2:mem:sembradodiciembre"})
@Import(RelojFijoTestConfig.class)
class SembradoEnDiciembreTest extends SembradoConRelojFijoBase {

    @Override
    LocalDate hoyEsperado() {
        return LocalDate.of(2027, 12, 10);
    }
}
