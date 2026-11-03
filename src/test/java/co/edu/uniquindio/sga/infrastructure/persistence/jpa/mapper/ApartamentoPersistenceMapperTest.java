package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApartamentoPersistenceMapperTest {

    private final ApartamentoPersistenceMapper mapper = new ApartamentoPersistenceMapper();

    @Test
    void ida_y_vuelta_conserva_todos_los_datos_de_un_apartamento_activo_fuera_de_servicio() {
        Apartamento original = Apartamento.reconstruir(new IdentificacionApartamento("apt-101"), "Vista al lago",
                3, 6, EstadoOperativo.FUERA_DE_SERVICIO, true);

        Apartamento vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertEquals(original, vuelta);
        assertEquals("Vista al lago", vuelta.getNombre());
        assertEquals(3, vuelta.getDormitorios());
        assertEquals(6, vuelta.getCapacidad());
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, vuelta.getEstadoOperativo());
        assertTrue(vuelta.isActivo());
    }

    @Test
    void un_apartamento_inactivo_se_restaura_inactivo() {
        Apartamento nuevo = new Apartamento(new IdentificacionApartamento("APT-1"), "Nuevo", 1, 2);

        assertFalse(mapper.aDominio(mapper.aEntidad(nuevo)).isActivo());
    }

    @Test
    void la_entidad_toma_la_identificacion_normalizada_como_clave() {
        ApartamentoEntity entidad = mapper.aEntidad(
                new Apartamento(new IdentificacionApartamento(" apt-7 "), "X", 1, 1));

        assertEquals("APT-7", entidad.getIdentificacion());
    }
}
