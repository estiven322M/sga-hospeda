package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloqueoPersistenceMapperTest {

    private final BloqueoPersistenceMapper mapper = new BloqueoPersistenceMapper(new EstanciaPersistenceMapper());

    @Test
    void ida_y_vuelta_conserva_apartamento_rango_y_motivo() {
        Bloqueo original = new Bloqueo(new IdentificacionApartamento("apt-101"),
                new Estancia(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4)), "Mantenimiento");

        BloqueoEntity entidad = mapper.aEntidad(original);
        Bloqueo vuelta = mapper.aDominio(entidad);

        assertEquals("APT-101", entidad.getApartamentoId());
        assertEquals(original.getApartamento(), vuelta.getApartamento());
        assertEquals(original.getRango(), vuelta.getRango());
        assertEquals(original.getMotivo(), vuelta.getMotivo());
    }
}
