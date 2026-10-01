package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IdentificacionApartamentoTest {
	
	@Test
    void debeNormalizarAMayusculas() {
        IdentificacionApartamento id = new IdentificacionApartamento("apt-301");
        assertEquals("APT-301", id.valor());
    }

    @Test
    void dosIdentificacionesConElMismoValorNormalizadoSonIguales() {
        IdentificacionApartamento a = new IdentificacionApartamento("apt-301");
        IdentificacionApartamento b = new IdentificacionApartamento("APT-301");
        assertEquals(a, b);
    }

    @Test
    void noDebeCrearIdentificacionVacia() {
        assertThrows(ReglaDominioException.class, () -> new IdentificacionApartamento("   "));
    }
	

}
