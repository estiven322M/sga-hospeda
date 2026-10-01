package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

public class FolioTest {
	
	@Test
    void debeCalcularSaldoComoDiferenciaDeCargosYPagosRN15() {
        // Arrange: El folio se abre exigiendo la reserva y el cargo base de alojamiento
        Folio folio = new Folio(new CodigoReserva("RES-2026-00001"), new Cargo("Alojamiento", Dinero.de(100000)));
        
        // Act: Creamos el pago y lo registramos en el folio
        Pago pago = new Pago("Efectivo", LocalDate.of(2026, 1, 1), Dinero.de(40000));
        folio.registrarPago(pago);

        // Assert: El saldo debe ser la diferencia exacta (100.000 - 40.000 = 60.000)
        assertEquals(Dinero.de(60000), folio.saldo());
    }

    @Test
    void noDebeCerrarConSaldoPendienteSinAutorizacionRN17() {
        Folio folio = new Folio(new CodigoReserva("RES-2026-00001"), new Cargo("Alojamiento", Dinero.de(100000)));

        // Intentamos cerrar sin enviar el texto de autorización
        assertThrows(ReglaDominioException.class, () -> folio.cerrar(null));
    }

    @Test
    void noDebePermitirModificarListaDeCargosDesdeAfueraRN16() {
        Folio folio = new Folio(new CodigoReserva("RES-2026-00001"), new Cargo("Alojamiento", Dinero.de(100000)));
        
        // Verificamos que la colección sea inmutable
        assertThrows(UnsupportedOperationException.class, () -> 
            folio.obtenerCargos().add(new Cargo("Trampa", Dinero.de(0)))
        );
    }

}
