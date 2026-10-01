package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class DineroTest {
	
	@Test
    void debeSoportarValoresNegativosParaSaldosAAFavor() {
        // Arrange & Act: El negocio explícitamente permite negativos
        Dinero ajuste = Dinero.de(-50000);
        
        // Assert
        assertTrue(ajuste.esNegativo());
    }

    @Test
    void debeSumarCorrectamenteYRetornarNuevoObjeto() {
        // Arrange
        Dinero base = Dinero.de(100000);
        Dinero adicion = Dinero.de(50000);

        // Act
        Dinero resultado = base.mas(adicion);

        // Assert
        assertEquals(Dinero.de(150000), resultado);
        // Prueba de inmutabilidad: el base original no debió cambiar
        assertEquals(Dinero.de(100000), base); 
    }

    @Test
    void debeMultiplicarCorrectamente() {
        Dinero tarifa = Dinero.de(80000);
        Dinero total = tarifa.por(3);
        
        assertEquals(Dinero.de(240000), total);
    }

    @Test
    void noDebePermitirValorNulo() {
        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> new Dinero(null));
        assertEquals("El valor monetario es obligatorio", ex.getMessage());
    }

}
