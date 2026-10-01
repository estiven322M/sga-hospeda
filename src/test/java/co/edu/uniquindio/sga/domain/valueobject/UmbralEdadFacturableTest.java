package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UmbralEdadFacturableTest {
	
	@Test
    void noDebePermitirUmbralNegativo() {
        assertThrows(ReglaDominioException.class, () -> new UmbralEdadFacturable(-1));
    }

    @Test
    void noDebePermitirUmbralMayorATreinta() {
        assertThrows(ReglaDominioException.class, () -> new UmbralEdadFacturable(31));
    }

    @Test
    void debePermitirUmbralesValidos() {
        assertDoesNotThrow(() -> new UmbralEdadFacturable(18));
        assertDoesNotThrow(() -> new UmbralEdadFacturable(0));
    }

}
