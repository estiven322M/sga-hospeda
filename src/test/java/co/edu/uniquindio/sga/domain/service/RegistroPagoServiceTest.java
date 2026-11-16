package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/** L-18: solo se registran pagos con un medio de pago aceptado por el alojamiento. */
class RegistroPagoServiceTest {

    private final ConfiguracionAlojamiento configuracion = mock(ConfiguracionAlojamiento.class);
    private RegistroPagoService servicio;
    private Folio folio;

    @BeforeEach
    void setUp() {
        when(configuracion.mediosPagoAceptados()).thenReturn(List.of("EFECTIVO", "TARJETA", "TRANSFERENCIA"));
        servicio = new RegistroPagoService(configuracion);
        folio = new Folio(new CodigoReserva("RES-2026-00001"), new Cargo("Alojamiento", Dinero.de(300_000)));
    }

    @Test
    void registraUnPagoConUnMedioAceptadoSinDistinguirMayusculasNiEspacios() {
        servicio.registrar(folio, new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(100_000)));
        servicio.registrar(folio, new Pago(" tarjeta ", LocalDate.of(2026, 11, 3), Dinero.de(50_000)));

        assertEquals(2, folio.obtenerPagos().size());
        assertEquals(Dinero.de(150_000), folio.saldo());
    }

    @Test
    void rechazaUnMedioDePagoNoAceptadoConLaListaDeLosAceptados() {
        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> servicio.registrar(folio, new Pago("CRIPTOMONEDA", LocalDate.of(2026, 11, 3), Dinero.de(100_000))));

        assertEquals("El medio de pago CRIPTOMONEDA no es aceptado; los medios aceptados son: EFECTIVO, TARJETA, TRANSFERENCIA", e.getMessage());
        assertEquals(0, folio.obtenerPagos().size()); // no se registró nada
    }

    @Test
    void unFolioCerradoSigueRechazandoElPagoAunqueElMedioSeaValido() {
        folio.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(300_000)));
        folio.cerrar(null);

        assertThrows(ReglaDominioException.class,
                () -> servicio.registrar(folio, new Pago("EFECTIVO", LocalDate.of(2026, 11, 4), Dinero.de(1))));
    }
}
