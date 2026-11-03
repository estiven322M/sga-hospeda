package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.PoliticaCancelacionPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.TarifaPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.TemporadaPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({TemporadaJpaRepository.class, TarifarioJpaRepository.class, PoliticaCancelacionJpaRepository.class,
        TemporadaPersistenceMapper.class, TarifaPersistenceMapper.class, PoliticaCancelacionPersistenceMapper.class})
class TarifarioPersistenciaTest {

    private static final IdentificacionApartamento APT = new IdentificacionApartamento("APT-101");
    private static final LocalDate INICIO_ALTA = LocalDate.of(2026, 12, 15);
    private static final LocalDate FIN_ALTA = LocalDate.of(2027, 1, 15);

    @Autowired
    private TemporadaJpaRepository temporadas;
    @Autowired
    private TarifarioJpaRepository tarifario;
    @Autowired
    private PoliticaCancelacionJpaRepository politicas;

    private void sembrarTemporadas() {
        temporadas.guardar(new Temporada("BASE", true, List.of()));
        temporadas.guardar(new Temporada("ALTA", false, List.of(new Periodo(INICIO_ALTA, FIN_ALTA))));
    }

    private static PoliticaCancelacion politica(int version, LocalDate desde) {
        return new PoliticaCancelacion(new VersionPolitica(version), desde,
                List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 100)));
    }

    // --- Temporadas ---

    @Test
    void listar_temporadas_devuelve_todas_con_sus_periodos_ordenadas_por_nombre() {
        sembrarTemporadas();

        List<Temporada> todas = temporadas.listarTodas();

        assertEquals(List.of("ALTA", "BASE"), todas.stream().map(Temporada::getNombre).toList());
        assertEquals(List.of(new Periodo(INICIO_ALTA, FIN_ALTA)), todas.get(0).getPeriodos());
        assertTrue(todas.get(1).getPeriodos().isEmpty());
    }

    @Test
    void guardar_una_temporada_existente_reemplaza_sus_periodos_sin_duplicarla() {
        sembrarTemporadas();
        Periodo nuevo = new Periodo(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 30));

        temporadas.guardar(new Temporada("ALTA", false, List.of(nuevo)));

        assertEquals(2, temporadas.listarTodas().size());
        Temporada alta = temporadas.listarTodas().get(0);
        assertEquals(List.of(nuevo), alta.getPeriodos());
    }

    // --- Tarifas ---

    @Test
    void la_tarifa_de_una_noche_dentro_del_periodo_usa_la_temporada_que_la_reclama() {
        sembrarTemporadas();
        tarifario.guardarTarifa(APT, new Tarifa("BASE", Dinero.de(100_000)));
        tarifario.guardarTarifa(APT, new Tarifa("ALTA", Dinero.de(180_000)));

        assertEquals("ALTA", tarifario.tarifaDe(APT, LocalDate.of(2026, 12, 25)).temporada());
    }

    @Test
    void los_extremos_del_periodo_son_inclusivos_y_fuera_de_el_aplica_la_base() {
        sembrarTemporadas();
        tarifario.guardarTarifa(APT, new Tarifa("BASE", Dinero.de(100_000)));
        tarifario.guardarTarifa(APT, new Tarifa("ALTA", Dinero.de(180_000)));

        assertEquals("ALTA", tarifario.tarifaDe(APT, INICIO_ALTA).temporada());
        assertEquals("ALTA", tarifario.tarifaDe(APT, FIN_ALTA).temporada());
        assertEquals("BASE", tarifario.tarifaDe(APT, INICIO_ALTA.minusDays(1)).temporada());
        assertEquals("BASE", tarifario.tarifaDe(APT, FIN_ALTA.plusDays(1)).temporada());
    }

    @Test
    void tarifa_de_devuelve_nulo_si_el_apartamento_no_tiene_tarifa_en_esa_temporada() {
        sembrarTemporadas();
        tarifario.guardarTarifa(APT, new Tarifa("BASE", Dinero.de(100_000)));

        assertNull(tarifario.tarifaDe(APT, LocalDate.of(2026, 12, 25)));
        assertNull(tarifario.tarifaDe(new IdentificacionApartamento("APT-999"), LocalDate.of(2026, 3, 1)));
    }

    @Test
    void guardar_la_tarifa_de_una_temporada_existente_la_reemplaza() {
        sembrarTemporadas();
        tarifario.guardarTarifa(APT, new Tarifa("BASE", Dinero.de(100_000)));

        tarifario.guardarTarifa(APT, new Tarifa("BASE", Dinero.de(120_000)));

        List<Tarifa> tarifas = tarifario.listarTarifas(APT);
        assertEquals(1, tarifas.size());
        assertEquals(Dinero.de(120_000), tarifas.get(0).valorPorOcupanteNoche());
    }

    @Test
    void listar_tarifas_devuelve_solo_las_del_apartamento_ordenadas_por_temporada() {
        sembrarTemporadas();
        tarifario.guardarTarifa(APT, new Tarifa("BASE", Dinero.de(100_000)));
        tarifario.guardarTarifa(APT, new Tarifa("ALTA", Dinero.de(180_000)));
        tarifario.guardarTarifa(new IdentificacionApartamento("APT-102"), new Tarifa("BASE", Dinero.de(90_000)));

        assertEquals(List.of("ALTA", "BASE"),
                tarifario.listarTarifas(APT).stream().map(Tarifa::temporada).toList());
        assertTrue(tarifario.listarTarifas(new IdentificacionApartamento("APT-777")).isEmpty());
    }

    // --- Política de cancelación ---

    @Test
    void vigente_en_devuelve_la_de_mayor_vigencia_que_no_supera_la_fecha() {
        politicas.guardar(politica(1, LocalDate.of(2026, 1, 1)));
        politicas.guardar(politica(2, LocalDate.of(2026, 6, 1)));
        politicas.guardar(politica(3, LocalDate.of(2027, 1, 1)));

        assertEquals(2, politicas.vigenteEn(LocalDate.of(2026, 11, 2)).orElseThrow().getVersion().numero());
        assertEquals(2, politicas.vigenteEn(LocalDate.of(2026, 6, 1)).orElseThrow().getVersion().numero());
        assertEquals(1, politicas.vigenteEn(LocalDate.of(2026, 5, 31)).orElseThrow().getVersion().numero());
    }

    @Test
    void vigente_en_una_fecha_anterior_a_toda_politica_devuelve_vacio() {
        politicas.guardar(politica(1, LocalDate.of(2026, 1, 1)));

        assertTrue(politicas.vigenteEn(LocalDate.of(2025, 12, 31)).isEmpty());
    }

    @Test
    void ultima_version_es_la_de_mayor_numero_y_vacio_si_no_hay_ninguna() {
        assertTrue(politicas.ultimaVersion().isEmpty());
        politicas.guardar(politica(1, LocalDate.of(2026, 1, 1)));
        politicas.guardar(politica(2, LocalDate.of(2026, 6, 1)));

        assertEquals(2, politicas.ultimaVersion().orElseThrow().getVersion().numero());
    }

    @Test
    void por_version_devuelve_la_politica_con_sus_tramos_o_vacio() {
        politicas.guardar(politica(1, LocalDate.of(2026, 1, 1)));

        PoliticaCancelacion encontrada = politicas.porVersion(new VersionPolitica(1)).orElseThrow();

        assertEquals(List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 100)), encontrada.getTramos());
        assertTrue(politicas.porVersion(new VersionPolitica(9)).isEmpty());
    }
}
