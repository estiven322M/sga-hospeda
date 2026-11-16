package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApartamentoPersistenceMapperTest {

    private final ApartamentoPersistenceMapper mapper = new ApartamentoPersistenceMapper();

    @Test
    void ida_y_vuelta_conserva_todos_los_datos_de_un_apartamento_activo_fuera_de_servicio() {
        Apartamento original = Apartamento.reconstruir(new IdentificacionApartamento("apt-101"), "Vista al lago",
                "Con vista al lago", 3, 6, List.of("Nevera", "Wi-Fi"),
                Set.of(Caracteristica.TERRAZA, Caracteristica.JACUZZI), EstadoOperativo.FUERA_DE_SERVICIO, true);

        Apartamento vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertEquals(original, vuelta);
        assertEquals("Vista al lago", vuelta.getNombre());
        assertEquals("Con vista al lago", vuelta.getDescripcion());
        assertEquals(3, vuelta.getDormitorios());
        assertEquals(6, vuelta.getCapacidad());
        assertEquals(List.of("Nevera", "Wi-Fi"), vuelta.getDotacion());
        assertEquals(Set.of(Caracteristica.TERRAZA, Caracteristica.JACUZZI), vuelta.getCaracteristicas());
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, vuelta.getEstadoOperativo());
        assertTrue(vuelta.isActivo());
    }

    @Test
    void un_apartamento_inactivo_se_restaura_inactivo() {
        Apartamento nuevo = ApartamentoDePrueba.nuevo(new IdentificacionApartamento("APT-1"), "Nuevo", 1, 2);

        assertFalse(mapper.aDominio(mapper.aEntidad(nuevo)).isActivo());
    }

    @Test
    void la_entidad_toma_la_identificacion_normalizada_como_clave() {
        ApartamentoEntity entidad = mapper.aEntidad(
                ApartamentoDePrueba.nuevo(new IdentificacionApartamento(" apt-7 "), "X", 1, 1));

        assertEquals("APT-7", entidad.getIdentificacion());
    }

    @Test
    void la_entidad_guarda_la_descripcion_la_dotacion_y_las_caracteristicas() {
        Apartamento apartamento = new Apartamento(new IdentificacionApartamento("APT-1"), "X", "Descripción", 1, 2,
                List.of("Nevera"), Set.of(Caracteristica.BALCON));

        ApartamentoEntity entidad = mapper.aEntidad(apartamento);

        assertEquals("Descripción", entidad.getDescripcion());
        assertEquals(List.of("Nevera"), entidad.getDotacion());
        assertEquals(Set.of(Caracteristica.BALCON), entidad.getCaracteristicas());
    }
}
