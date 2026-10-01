package co.edu.uniquindio.sga.domain.valueobject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EstadoOperativoTest {
	
	@Test
    void debePermitirTransicionesLegales() {
        assertTrue(EstadoOperativo.PREPARADO.puedeTransicionarA(EstadoOperativo.OCUPADO));
        assertTrue(EstadoOperativo.OCUPADO.puedeTransicionarA(EstadoOperativo.PENDIENTE_PREPARACION));
    }

    @Test
    void noDebePermitirTransicionesIlegales() {
        assertFalse(EstadoOperativo.OCUPADO.puedeTransicionarA(EstadoOperativo.PREPARADO));
        assertFalse(EstadoOperativo.PREPARADO.puedeTransicionarA(EstadoOperativo.EN_PREPARACION));
    }

    @Test
    void soloPreparadoPermiteRegistroRN11() {
        assertTrue(EstadoOperativo.PREPARADO.permiteRegistro());
        assertFalse(EstadoOperativo.OCUPADO.permiteRegistro());
    }

}
