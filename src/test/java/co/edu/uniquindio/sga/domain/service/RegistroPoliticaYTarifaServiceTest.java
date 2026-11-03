package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** F-07 (política versionada) y 7.4 (la tarifa pertenece a una temporada que existe). */
class RegistroPoliticaYTarifaServiceTest {

    private final PoliticaCancelacionRepository politicas = mock(PoliticaCancelacionRepository.class);
    private final TemporadaRepository temporadas = mock(TemporadaRepository.class);
    private final RegistroPoliticaCancelacionService registroPolitica = new RegistroPoliticaCancelacionService(politicas);
    private final RegistroTarifaService registroTarifa = new RegistroTarifaService(temporadas);

    private final List<TramoRetencion> tramos = List.of(new TramoRetencion(48, 0), new TramoRetencion(0, 100));

    // ---------- Política ----------

    @Test
    void laPrimeraPoliticaEsLaVersionUno() {
        when(politicas.ultimaVersion()).thenReturn(Optional.empty());

        PoliticaCancelacion p = registroPolitica.crearNuevaVersion(LocalDate.of(2026, 1, 1), tramos);

        assertEquals(new VersionPolitica(1), p.getVersion());
    }

    @Test
    void lasSiguientesIncrementanLaVersionYNoModificanLaAnterior() {
        PoliticaCancelacion anterior = new PoliticaCancelacion(new VersionPolitica(3), LocalDate.of(2026, 1, 1), tramos);
        when(politicas.ultimaVersion()).thenReturn(Optional.of(anterior));

        PoliticaCancelacion nueva = registroPolitica.crearNuevaVersion(LocalDate.of(2026, 6, 1), tramos);

        assertEquals(new VersionPolitica(4), nueva.getVersion());
        assertEquals(new VersionPolitica(3), anterior.getVersion());
    }

    @Test
    void laNuevaVersionDebeRegirDespuesDeLaAnterior() {
        PoliticaCancelacion anterior = new PoliticaCancelacion(new VersionPolitica(1), LocalDate.of(2026, 6, 1), tramos);
        when(politicas.ultimaVersion()).thenReturn(Optional.of(anterior));

        assertThrows(ReglaDominioException.class,
                () -> registroPolitica.crearNuevaVersion(LocalDate.of(2026, 6, 1), tramos));
        assertThrows(ReglaDominioException.class,
                () -> registroPolitica.crearNuevaVersion(LocalDate.of(2026, 5, 1), tramos));
    }

    @Test
    void sigueExigiendoDosTramosYUnoDeAntelacionCero() {
        when(politicas.ultimaVersion()).thenReturn(Optional.empty());

        assertThrows(ReglaDominioException.class, () -> registroPolitica.crearNuevaVersion(
                LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(0, 100))));
        assertThrows(ReglaDominioException.class, () -> registroPolitica.crearNuevaVersion(
                LocalDate.of(2026, 1, 1), List.of(new TramoRetencion(48, 0), new TramoRetencion(24, 50))));
        assertThrows(ReglaDominioException.class, () -> registroPolitica.crearNuevaVersion(null, tramos));
    }

    // ---------- Tarifa ----------

    private void conTemporadas() {
        when(temporadas.listarTodas()).thenReturn(List.of(
                new Temporada("BASE", true, List.of()),
                new Temporada("ALTA", false, List.of(new Periodo(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15))))));
    }

    @Test
    void creaLaTarifaParaUnaTemporadaExistenteIncluidaLaBase() {
        conTemporadas();

        assertEquals(new Tarifa("ALTA", Dinero.de(120_000)), registroTarifa.crear(" ALTA ", Dinero.de(120_000)));
        assertEquals("BASE", registroTarifa.crear("BASE", Dinero.de(80_000)).temporada());
    }

    @Test
    void rechazaUnaTemporadaInexistente() {
        conTemporadas();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> registroTarifa.crear("FERIADOS", Dinero.de(100_000)));

        assertEquals("La temporada FERIADOS no existe", ex.getMessage());
    }

    @Test
    void rechazaTarifasNegativasOSinTemporadaSinConsultarTemporadas() {
        assertThrows(ReglaDominioException.class, () -> registroTarifa.crear("BASE", Dinero.de(-1)));
        assertThrows(ReglaDominioException.class, () -> registroTarifa.crear("BASE", null));
        assertThrows(ReglaDominioException.class, () -> registroTarifa.crear(" ", Dinero.de(1)));

        verifyNoInteractions(temporadas);
    }
}
