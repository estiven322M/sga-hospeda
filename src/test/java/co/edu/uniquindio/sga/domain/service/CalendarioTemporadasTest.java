package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;

/** 7.4: temporadas sin solapes y con una única temporada base que cubre las fechas restantes. */
class CalendarioTemporadasTest {

    private static Temporada base() {
        return new Temporada("BASE", true, List.of());
    }

    private static Temporada alta() {
        return new Temporada("ALTA", false,
                List.of(new Periodo(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15))));
    }

    @Test
    void laNocheDentroDeUnaTemporadaTomaEsaTemporada() {
        var calendario = new CalendarioTemporadas(List.of(base(), alta()));
        assertEquals("ALTA", calendario.temporadaDe(LocalDate.of(2026, 12, 31)).getNombre());
        assertEquals("ALTA", calendario.temporadaDe(LocalDate.of(2026, 12, 15)).getNombre()); // extremo inclusivo
    }

    @Test
    void laNocheFueraDeLasTemporadasTomaLaBase() {
        var calendario = new CalendarioTemporadas(List.of(base(), alta()));
        assertEquals("BASE", calendario.temporadaDe(LocalDate.of(2026, 3, 1)).getNombre());
    }

    @Test
    void rechazaTemporadasSolapadas() {
        Temporada otra = new Temporada("FERIADOS", false,
                List.of(new Periodo(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 20))));
        assertThrows(ReglaDominioException.class, () -> new CalendarioTemporadas(List.of(base(), alta(), otra)));
    }

    @Test
    void exigeExactamenteUnaTemporadaBase() {
        assertThrows(ReglaDominioException.class, () -> new CalendarioTemporadas(List.of(alta())));
        assertThrows(ReglaDominioException.class,
                () -> new CalendarioTemporadas(List.of(base(), new Temporada("BASE2", true, List.of()))));
    }

    @Test
    void rechazaNombresRepetidos() {
        assertThrows(ReglaDominioException.class, () -> new CalendarioTemporadas(List.of(base(), base())));
    }

    @Test
    void unaTemporadaNoBaseRequierePeriodosYLaBaseNoLosAdmite() {
        assertThrows(ReglaDominioException.class, () -> new Temporada("ALTA", false, List.of()));
        assertThrows(ReglaDominioException.class, () -> new Temporada("BASE", true,
                List.of(new Periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)))));
    }

    @Test
    void elPeriodoNoPuedeTerminarAntesDeEmpezar() {
        assertThrows(ReglaDominioException.class,
                () -> new Periodo(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)));
    }
}
