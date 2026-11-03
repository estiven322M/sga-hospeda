package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/** F-07, L-10, RN-13: retención según tramos de antelación. */
class PoliticaCancelacionTest {

    private static PoliticaCancelacion politica() {
        // 7 días o más: 0 %; entre 2 y 7 días: 50 %; menos de 2 días: 100 %
        return new PoliticaCancelacion(new VersionPolitica(1), LocalDate.of(2026, 1, 1), List.of(
                new TramoRetencion(168, 0), new TramoRetencion(48, 50), new TramoRetencion(0, 100)));
    }

    @Test
    void conMuchaAntelacionNoRetiene() {
        assertEquals(Dinero.CERO, politica().retencion(Dinero.de(1_000_000), Duration.ofDays(10)));
    }

    @Test
    void enElTramoIntermedioRetieneElPorcentaje() {
        assertEquals(Dinero.de(500_000), politica().retencion(Dinero.de(1_000_000), Duration.ofDays(3)));
        assertEquals(Dinero.de(500_000), politica().devolucion(Dinero.de(1_000_000), Duration.ofDays(3)));
    }

    @Test
    void conPocaOCeroAntelacionRetieneTodo() {
        assertEquals(Dinero.de(1_000_000), politica().retencion(Dinero.de(1_000_000), Duration.ofHours(5)));
        assertEquals(Dinero.de(1_000_000), politica().retencion(Dinero.de(1_000_000), Duration.ofHours(-3)));
    }

    @Test
    void laRetencionSeRedondeaAlPeso() {
        var p = new PoliticaCancelacion(new VersionPolitica(1), LocalDate.of(2026, 1, 1),
                List.of(new TramoRetencion(24, 33), new TramoRetencion(0, 100)));
        // 33 % de 1.005 = 331,65 -> 332 (HALF_UP al peso)
        assertEquals(Dinero.de(332), p.retencion(Dinero.de(1_005), Duration.ofDays(2)));
    }

    @Test
    void exigeAlMenosDosTramos() {
        assertThrows(ReglaDominioException.class, () -> new PoliticaCancelacion(new VersionPolitica(1),
                LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(0, 100))));
    }

    @Test
    void exigeUnTramoDeAntelacionCeroYSinRepetidos() {
        assertThrows(ReglaDominioException.class, () -> new PoliticaCancelacion(new VersionPolitica(1),
                LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(48, 0), new TramoRetencion(24, 50))));
        assertThrows(ReglaDominioException.class, () -> new PoliticaCancelacion(new VersionPolitica(1),
                LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(0, 0), new TramoRetencion(0, 50))));
    }

    @Test
    void elPorcentajeDebeEstarEntreCeroYCien() {
        assertThrows(ReglaDominioException.class, () -> new TramoRetencion(0, 101));
        assertThrows(ReglaDominioException.class, () -> new TramoRetencion(0, -1));
    }
}
