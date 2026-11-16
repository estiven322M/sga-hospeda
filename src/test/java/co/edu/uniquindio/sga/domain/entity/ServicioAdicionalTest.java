package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/** L-17: el servicio adicional, su coherencia entre "genera cargo" y su valor, y su baja lógica. */
class ServicioAdicionalTest {

    private static ServicioAdicional desayuno() {
        return new ServicioAdicional("desayuno", "Desayuno típico", "Por persona", true, Dinero.de(25_000));
    }

    @Test
    void naceActivoConElCodigoEnMayusculasYSeIdentificaPorElCodigo() {
        ServicioAdicional s = desayuno();

        assertEquals("DESAYUNO", s.getCodigo());
        assertTrue(s.isActivo());
        assertTrue(s.generaCargo());
        assertEquals(Dinero.de(25_000), s.getValor());
        assertEquals(s, new ServicioAdicional("DESAYUNO", "Otro nombre", "", true, Dinero.de(1)));
    }

    @Test
    void unServicioQueGeneraCargoDebeCostarAlgoYUnaCortesiaNoLlevaValor() {
        ReglaDominioException sinValor = assertThrows(ReglaDominioException.class,
                () -> new ServicioAdicional("LAV", "Lavandería", "", true, Dinero.CERO));
        assertEquals("Un servicio adicional que genera cargo debe tener un valor mayor que cero", sinValor.getMessage());
        ReglaDominioException cortesiaConValor = assertThrows(ReglaDominioException.class,
                () -> new ServicioAdicional("BIEN", "Bienvenida", "", false, Dinero.de(5_000)));
        assertEquals("Un servicio adicional que no genera cargo no puede tener valor", cortesiaConValor.getMessage());

        ServicioAdicional cortesia = new ServicioAdicional("BIEN", "Bienvenida", "", false, Dinero.CERO);
        assertFalse(cortesia.generaCargo());
    }

    @Test
    void exigeCodigoNombreYValorNoNegativo() {
        assertThrows(ReglaDominioException.class, () -> new ServicioAdicional(" ", "N", "", true, Dinero.de(1)));
        assertThrows(ReglaDominioException.class, () -> new ServicioAdicional("C", " ", "", true, Dinero.de(1)));
        assertThrows(ReglaDominioException.class, () -> new ServicioAdicional("C", "N", "", true, Dinero.de(-1)));
        assertThrows(ReglaDominioException.class, () -> new ServicioAdicional("C", "N", "", true, null));
        assertThrows(ReglaDominioException.class, () -> new ServicioAdicional("C".repeat(31), "N", "", true, Dinero.de(1)));
    }

    @Test
    void laBajaEsLogicaYNoSePuedeRepetirNiReactivarUnoActivo() {
        ServicioAdicional s = desayuno();

        s.darDeBaja();
        assertFalse(s.isActivo());
        ReglaDominioException yaDeBaja = assertThrows(ReglaDominioException.class, s::darDeBaja);
        assertEquals("El servicio adicional DESAYUNO ya está dado de baja", yaDeBaja.getMessage());

        s.activar();
        assertTrue(s.isActivo());
        assertThrows(ReglaDominioException.class, s::activar);
    }

    @Test
    void actualizarEsAtomicaYNoCambiaElCodigo() {
        ServicioAdicional s = desayuno();

        s.actualizarDatos("Desayuno completo", "Con jugo", true, Dinero.de(30_000));
        assertEquals("Desayuno completo", s.getNombre());
        assertEquals(Dinero.de(30_000), s.getValor());
        assertEquals("DESAYUNO", s.getCodigo());

        assertThrows(ReglaDominioException.class, () -> s.actualizarDatos("Otro", "", true, Dinero.CERO));
        assertEquals("Desayuno completo", s.getNombre()); // no se aplicó nada
    }
}
