package co.edu.uniquindio.sga.infrastructure.config;

import java.time.LocalDate;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.sga.testsupport.RelojFijoTestConfig;

/**
 * Arranque el 5 de junio de 2027: la reserva sembrada a +10 días (15 al 18 de junio) y la de +20 días (25 al 28) caen
 * en la temporada ALTA (15-jun a 15-jul), donde RP-01 exige al menos 3 noches.
 */
@SpringBootTest(properties = {"test.reloj-fijo=2027-06-05T12:00", "spring.datasource.url=jdbc:h2:mem:sembradojunio"})
@Import(RelojFijoTestConfig.class)
class SembradoEnJunioTest extends SembradoConRelojFijoBase {

    @Override
    LocalDate hoyEsperado() {
        return LocalDate.of(2027, 6, 5);
    }
}
