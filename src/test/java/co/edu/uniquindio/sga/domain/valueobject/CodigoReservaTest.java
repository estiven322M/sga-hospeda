package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CodigoReservaTest {
	
	@Test
    void dosCodigosConElMismoValorSonIguales() {
        // Arrange & Act
		CodigoReserva a = new CodigoReserva("RES-2026-00001");
        CodigoReserva b = new CodigoReserva("RES-2026-00001");

        // Assert
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
	
	@Test
    void noDebeCrearCodigoVacioONulo() {
        ReglaDominioException excepcionNulo = assertThrows(ReglaDominioException.class, () -> {
            new CodigoReserva(null);
        });
        
        ReglaDominioException excepcionVacio = assertThrows(ReglaDominioException.class, () -> {
            new CodigoReserva("   ");
        });

        // El mensaje debe ser idéntico al que lanza CodigoReserva
        assertEquals("El código de la reserva es obligatorio", excepcionNulo.getMessage());
        assertEquals("El código de la reserva es obligatorio", excepcionVacio.getMessage());
    }
	
	//  prueba que valida el formato incorrecto!
    @Test
    void noDebePermitirFormatoInvalido() {
        ReglaDominioException excepcion = assertThrows(ReglaDominioException.class, () -> {
            new CodigoReserva("RES-001"); // Falta el año y dígitos
        });

        // Asegúrate de que este texto coincida exactamente con el que tienes en tu clase CodigoReserva
        assertEquals("El código de la reserva debe tener el formato RES-YYYY-NNNNN", excepcion.getMessage());
    }

}
