package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** 7.6: una novedad tiene apartamento, fecha, autor, descripción obligatoria de máximo 500 caracteres y gravedad. */
class NovedadTest {

    private static final IdentificacionApartamento APT = new IdentificacionApartamento("APT-101");
    private static final LocalDateTime FECHA = LocalDateTime.of(2027, 3, 10, 10, 0);

    private static Novedad novedad(String descripcion) {
        return new Novedad(APT, FECHA, "Personal de Servicio", descripcion, GravedadNovedad.MEDIA);
    }

    @Test
    void seRegistraConTodosSusDatosYSinEspaciosSobrantes() {
        Novedad novedad = new Novedad(APT, FECHA, "  Personal de Servicio ", "  Bombillo fundido en el baño  ",
                GravedadNovedad.BAJA);

        assertEquals(APT, novedad.apartamento());
        assertEquals(FECHA, novedad.fecha());
        assertEquals("Personal de Servicio", novedad.autor());
        assertEquals("Bombillo fundido en el baño", novedad.descripcion());
        assertEquals(GravedadNovedad.BAJA, novedad.gravedad());
    }

    @Test
    void laDescripcionEsObligatoria() {
        assertEquals("La descripción de la novedad es obligatoria",
                assertThrows(ReglaDominioException.class, () -> novedad(null)).getMessage());
        assertThrows(ReglaDominioException.class, () -> novedad(""));
        assertThrows(ReglaDominioException.class, () -> novedad("   "));
    }

    @Test
    void laDescripcionAdmiteHasta500Caracteres() {
        assertDoesNotThrow(() -> novedad("a".repeat(500)));
        // los espacios de los extremos no cuentan
        assertDoesNotThrow(() -> novedad(" " + "a".repeat(500) + " "));
    }

    @Test
    void laDescripcionDeMasDe500CaracteresSeRechaza() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> novedad("a".repeat(501)));

        assertEquals("La descripción de la novedad no puede superar 500 caracteres", error.getMessage());
    }

    @Test
    void laGravedadEsObligatoria() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> new Novedad(APT, FECHA, "Personal de Servicio", "Fuga de agua", null));

        assertEquals("La novedad debe indicar su gravedad", error.getMessage());
    }

    @Test
    void elAutorEsObligatorio() {
        assertThrows(ReglaDominioException.class, () -> new Novedad(APT, FECHA, null, "Fuga de agua", GravedadNovedad.ALTA));
        assertEquals("La novedad debe indicar su autor", assertThrows(ReglaDominioException.class,
                () -> new Novedad(APT, FECHA, "  ", "Fuga de agua", GravedadNovedad.ALTA)).getMessage());
    }

    @Test
    void elApartamentoYLaFechaSonObligatorios() {
        assertEquals("La novedad debe indicar el apartamento", assertThrows(ReglaDominioException.class,
                () -> new Novedad(null, FECHA, "Personal de Servicio", "Fuga de agua", GravedadNovedad.ALTA)).getMessage());
        assertEquals("La novedad debe tener fecha y hora", assertThrows(ReglaDominioException.class,
                () -> new Novedad(APT, null, "Personal de Servicio", "Fuga de agua", GravedadNovedad.ALTA)).getMessage());
    }

    @Test
    void lasGravedadesSonCuatroDeMenorAMayor() {
        assertEquals(java.util.List.of(GravedadNovedad.BAJA, GravedadNovedad.MEDIA, GravedadNovedad.ALTA,
                GravedadNovedad.CRITICA), java.util.List.of(GravedadNovedad.values()));
    }
}
