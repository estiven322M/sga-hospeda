package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EstanciaEmbeddable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.OcupanteEmbeddable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.RELOJ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstanciaYOcupantePersistenceMapperTest {

    private static final LocalDate HOY = LocalDate.now(RELOJ);

    private final EstanciaPersistenceMapper estancias = new EstanciaPersistenceMapper();
    private final OcupantePersistenceMapper ocupantes = new OcupantePersistenceMapper(RELOJ);

    @Test
    void estancia_ida_y_vuelta_conserva_entrada_y_salida() {
        Estancia original = new Estancia(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13));

        EstanciaEmbeddable embeddable = estancias.aEmbeddable(original);

        assertEquals(LocalDate.of(2027, 3, 10), embeddable.fechaEntrada());
        assertEquals(LocalDate.of(2027, 3, 13), embeddable.fechaSalida());
        assertEquals(original, estancias.aDominio(embeddable));
    }

    @Test
    void estancia_guardada_sin_noches_es_rechazada_por_el_dominio_al_restaurarla() {
        EstanciaEmbeddable corrupta = new EstanciaEmbeddable(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 10));

        assertThrows(ReglaDominioException.class, () -> estancias.aDominio(corrupta));
    }

    @Test
    void ocupante_ida_y_vuelta_conserva_documento_nombre_y_fecha_de_nacimiento() {
        Ocupante original = new Ocupante(new DocumentoIdentidad("CC-1001"), "Ana Ramírez",
                LocalDate.of(1990, 5, 12), HOY);

        OcupanteEmbeddable embeddable = ocupantes.aEmbeddable(original);
        Ocupante vuelta = ocupantes.aDominio(embeddable);

        assertEquals("CC-1001", embeddable.documento());
        assertEquals(original, vuelta);
        assertEquals("Ana Ramírez", vuelta.getNombre());
        assertEquals(LocalDate.of(1990, 5, 12), vuelta.getFechaNacimiento());
    }

    @Test
    void ocupante_nacido_hoy_se_restaura_porque_la_fecha_actual_sale_del_reloj() {
        Ocupante vuelta = ocupantes.aDominio(new OcupanteEmbeddable("TI-1", "Recién nacido", HOY));

        assertEquals(HOY, vuelta.getFechaNacimiento());
    }

    @Test
    void ocupante_guardado_con_nacimiento_futuro_es_rechazado_por_el_dominio_al_restaurarlo() {
        OcupanteEmbeddable corrupto = new OcupanteEmbeddable("TI-2", "Futuro", HOY.plusDays(1));

        assertThrows(ReglaDominioException.class, () -> ocupantes.aDominio(corrupto));
    }
}
