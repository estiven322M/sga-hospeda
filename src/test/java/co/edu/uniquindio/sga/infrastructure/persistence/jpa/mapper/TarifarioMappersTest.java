package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TarifarioMappersTest {

    private final TemporadaPersistenceMapper temporadas = new TemporadaPersistenceMapper();
    private final TarifaPersistenceMapper tarifas = new TarifaPersistenceMapper();
    private final PoliticaCancelacionPersistenceMapper politicas = new PoliticaCancelacionPersistenceMapper();

    @Test
    void temporada_con_varios_periodos_conserva_el_orden_en_la_ida_y_vuelta() {
        Temporada alta = new Temporada("ALTA", false, List.of(
                new Periodo(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15)),
                new Periodo(LocalDate.of(2027, 6, 20), LocalDate.of(2027, 7, 10))));

        Temporada vuelta = temporadas.aDominio(temporadas.aEntidad(alta));

        assertEquals(alta, vuelta);
        assertEquals(alta.getPeriodos(), vuelta.getPeriodos());
    }

    @Test
    void temporada_base_vuelve_sin_periodos() {
        Temporada vuelta = temporadas.aDominio(temporadas.aEntidad(new Temporada("BASE", true, List.of())));

        assertTrue(vuelta.esBase());
        assertTrue(vuelta.getPeriodos().isEmpty());
    }

    @Test
    void tarifa_se_guarda_como_pesos_enteros_y_vuelve_igual() {
        TarifaEntity entidad = tarifas.aEntidad(new IdentificacionApartamento("apt-101"),
                new Tarifa("ALTA", Dinero.de(185_000)));

        assertEquals(185_000L, entidad.getValorPorOcupanteNoche());
        assertEquals("APT-101", entidad.getId().apartamentoId());
        assertEquals(new Tarifa("ALTA", Dinero.de(185_000)), tarifas.aDominio(entidad));
    }

    @Test
    void politica_conserva_version_vigencia_y_tramos_en_orden() {
        PoliticaCancelacion politica = new PoliticaCancelacion(new VersionPolitica(2), LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(0, 100), new TramoRetencion(72, 0), new TramoRetencion(24, 50)));

        PoliticaCancelacion vuelta = politicas.aDominio(politicas.aEntidad(politica));

        assertEquals(politica, vuelta);
        assertEquals(politica.getVigenteDesde(), vuelta.getVigenteDesde());
        assertEquals(politica.getTramos(), vuelta.getTramos());
    }
}
