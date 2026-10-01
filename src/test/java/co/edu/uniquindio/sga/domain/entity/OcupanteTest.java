package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class OcupanteTest {
	
	@Test
    void debeSerFacturableSiAlcanzaElUmbralALaFechaDeEntradaRN06() {
        Ocupante ocupante = new Ocupante(new DocumentoIdentidad("123"), "Ana",
                LocalDate.of(2008, 5, 20), LocalDate.of(2026, 1, 1));
        Estancia estancia = new Estancia(LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 27));

        assertTrue(ocupante.esFacturableEn(estancia, new UmbralEdadFacturable(18)));
    }

    @Test
    void noDebeSerFacturableSiAunNoAlcanzaElUmbralALaFechaDeEntradaRN06() {
        Ocupante ocupante = new Ocupante(new DocumentoIdentidad("123"), "Ana",
                LocalDate.of(2008, 5, 20), LocalDate.of(2026, 1, 1));
        Estancia estancia = new Estancia(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12));

        assertFalse(ocupante.esFacturableEn(estancia, new UmbralEdadFacturable(18)));
    }

}
