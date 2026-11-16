package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** RP-02 (L-20): descuento por estancia larga. */
class PoliticaDescuentoEstanciaLargaTest {

    private final PoliticaDescuentoEstanciaLarga politica = new PoliticaDescuentoEstanciaLarga(7, 10);

    @Test
    void conSeisNochesNoHayDescuento() {
        assertEquals(0, politica.porcentajePara(6));
    }

    @Test
    void conExactamenteSieteNochesSeAplicaElDiezPorCiento() {
        assertEquals(10, politica.porcentajePara(7));
    }

    @Test
    void conMasDeSieteNochesSigueSiendoElMismoPorcentajeSinAcumularse() {
        assertEquals(10, politica.porcentajePara(8));
        assertEquals(10, politica.porcentajePara(30));
    }

    @Test
    void unPorcentajeDeCeroDesactivaElDescuentoSeaCualSeaLaEstancia() {
        assertEquals(0, new PoliticaDescuentoEstanciaLarga(7, 0).porcentajePara(30));
        assertEquals(0, PoliticaDescuentoEstanciaLarga.SIN_DESCUENTO.porcentajePara(100));
    }

    @Test
    void unPorcentajeFueraDeRangoONochesNegativasNoSonValidos() {
        assertThrows(ReglaDominioException.class, () -> new PoliticaDescuentoEstanciaLarga(7, 101));
        assertThrows(ReglaDominioException.class, () -> new PoliticaDescuentoEstanciaLarga(7, -1));
        assertThrows(ReglaDominioException.class, () -> new PoliticaDescuentoEstanciaLarga(-1, 10));
    }

    @Test
    void elCienPorCientoYElCeroSonLosLimitesValidos() {
        assertEquals(100, new PoliticaDescuentoEstanciaLarga(1, 100).porcentajePara(1));
        assertEquals(0, new PoliticaDescuentoEstanciaLarga(1, 0).porcentajePara(1));
    }
}
