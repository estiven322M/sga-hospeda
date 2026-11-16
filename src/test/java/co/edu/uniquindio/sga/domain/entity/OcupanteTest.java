package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.valueobject.EdadAdultoResponsable;
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

    // ---------- RP-03 (L-20): adulto responsable ----------

    private final Estancia estanciaDel10Dic = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    private final EdadAdultoResponsable dieciocho = new EdadAdultoResponsable(18);

    private Ocupante nacidoEl(LocalDate nacimiento) {
        return new Ocupante(new DocumentoIdentidad("123"), "Ana", nacimiento, LocalDate.of(2026, 1, 1));
    }

    @Test
    void conDiecisieteAniosYTrescientosSesentaYCuatroDiasAunNoEsAdultoRP03() {
        // cumple 18 el 11-dic-2026: el día de la entrada (10-dic) le falta un día
        Ocupante ocupante = nacidoEl(LocalDate.of(2008, 12, 11));

        assertEquals(17, ocupante.edadA(estanciaDel10Dic.fechaEntrada()));
        assertFalse(ocupante.esAdultoEn(estanciaDel10Dic, dieciocho));
    }

    @Test
    void elDiaExactoEnQueCumpleDieciochoYaEsAdultoRP03() {
        Ocupante ocupante = nacidoEl(LocalDate.of(2008, 12, 10)); // cumple 18 el día de la entrada

        assertEquals(18, ocupante.edadA(estanciaDel10Dic.fechaEntrada()));
        assertTrue(ocupante.esAdultoEn(estanciaDel10Dic, dieciocho));
    }

    @Test
    void laEdadSeEvaluaALaFechaDeEntradaNoALaDeCreacionRP03() {
        Ocupante ocupante = nacidoEl(LocalDate.of(2008, 12, 11));
        Estancia unDiaDespues = new Estancia(LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 13));

        assertFalse(ocupante.esAdultoEn(estanciaDel10Dic, dieciocho));
        assertTrue(ocupante.esAdultoEn(unDiaDespues, dieciocho)); // con otra entrada, otra respuesta
    }

    @Test
    void unOcupanteDeQuinceAniosPagaPeroNoEsAdultoResponsableRP03() {
        Ocupante quince = nacidoEl(LocalDate.of(2011, 6, 1));

        assertTrue(quince.esFacturableEn(estanciaDel10Dic, new UmbralEdadFacturable(12)));  // RN-06: paga
        assertFalse(quince.esAdultoEn(estanciaDel10Dic, dieciocho));                         // RP-03: no responde
    }

    @Test
    void laEdadDelAdultoSaleDelParametroNoDeUnaConstanteRP03() {
        Ocupante veinte = nacidoEl(LocalDate.of(2006, 6, 1));

        assertTrue(veinte.esAdultoEn(estanciaDel10Dic, new EdadAdultoResponsable(18)));
        assertFalse(veinte.esAdultoEn(estanciaDel10Dic, new EdadAdultoResponsable(21)));
    }
}
