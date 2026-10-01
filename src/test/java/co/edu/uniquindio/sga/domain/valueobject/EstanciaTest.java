package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class EstanciaTest {
	
	@Test
    void debeTenerAlMenosUnaNocheRN03() {
        LocalDate fecha = LocalDate.of(2026, 12, 10);

        // Intento de salida el mismo día (0 noches)
        assertThrows(ReglaDominioException.class, () -> new Estancia(fecha, fecha));

        // Intento de salida en el pasado (noches negativas)
        assertThrows(ReglaDominioException.class, () -> new Estancia(fecha, fecha.minusDays(1)));
    }

    @Test
    void debeCalcularNochesCorrectamente() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 14));
        // Del 10 al 14 hay 4 noches facturables
        assertEquals(4, estancia.noches());
    }

    @Test
    void debeDetectarSolapamientoConOtraEstancia() {
        Estancia original = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15));

        // Solapa: entra antes y sale en medio de la original
        Estancia solapa = new Estancia(LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 12));
        assertTrue(original.seSolapaCon(solapa));

        // No solapa: sale el mismo día que la original entra
        Estancia contiguaAntes = new Estancia(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 10));
        assertFalse(original.seSolapaCon(contiguaAntes));
    }

}
