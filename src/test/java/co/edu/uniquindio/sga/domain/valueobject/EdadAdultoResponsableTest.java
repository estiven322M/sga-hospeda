package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** RP-03 (L-20): la edad del adulto responsable es un parámetro, no una constante. */
class EdadAdultoResponsableTest {

    @Test
    void aceptaLosLimitesDelRango() {
        assertEquals(0, new EdadAdultoResponsable(0).anios());
        assertEquals(18, new EdadAdultoResponsable(18).anios());
        assertEquals(30, new EdadAdultoResponsable(30).anios());
    }

    @Test
    void rechazaUnaEdadNegativaOMayorDeTreintaAnios() {
        assertThrows(ReglaDominioException.class, () -> new EdadAdultoResponsable(-1));
        assertThrows(ReglaDominioException.class, () -> new EdadAdultoResponsable(31));
    }
}
