package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-05 y RN-06 en el cotizador: caminos de éxito y de violación. */
class CotizadorEstanciaReglasTest {

    private final TarifarioRepository tarifario = mock(TarifarioRepository.class);
    private final ConfiguracionAlojamiento config = mock(ConfiguracionAlojamiento.class);
    private final CotizadorEstancia cotizador = new CotizadorEstancia(tarifario, config);
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");
    private final LocalDate hoy = LocalDate.of(2026, 11, 2);
    private final Estancia estancia = new Estancia(LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 17));

    private Ocupante persona(String doc, LocalDate nacimiento) {
        return new Ocupante(new DocumentoIdentidad(doc), "Persona " + doc, nacimiento, hoy);
    }

    private final Ocupante adulto1 = persona("CC-1", LocalDate.of(1990, 1, 1));
    private final Ocupante adulto2 = persona("CC-2", LocalDate.of(1992, 1, 1));
    private final Ocupante nino = persona("TI-3", LocalDate.of(2021, 6, 1)); // 5 años

    @Test
    void soloLosOcupantesFacturablesGeneranCargoRN06() {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));
        when(tarifario.tarifaDe(eq(apt), any())).thenReturn(new Tarifa("BASE", Dinero.de(50_000)));

        ValorCongelado valor = cotizador.cotizar(apt, estancia, List.of(adulto1, adulto2, nino));

        assertTrue(valor.detalle().stream().allMatch(c -> c.ocupantesFacturables() == 2));
        assertEquals(Dinero.de(300_000), valor.total()); // 3 noches x 2 facturables x 50.000
    }

    @Test
    void unNinoQueNoAlcanzaElUmbralNoCuentaPeroSiCuentaParaLaCapacidadRN06() {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));
        when(tarifario.tarifaDe(eq(apt), any())).thenReturn(new Tarifa("BASE", Dinero.de(50_000)));

        // el niño no encarece la estancia: con o sin él, el valor es el mismo
        assertEquals(cotizador.cotizar(apt, estancia, List.of(adulto1)).total(),
                cotizador.cotizar(apt, estancia, List.of(adulto1, nino)).total());
    }

    @Test
    void rechazaUnGrupoSinNingunOcupanteFacturableRN06() {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> cotizador.cotizar(apt, estancia, List.of(nino)));

        assertEquals("La reserva debe tener al menos un ocupante facturable", ex.getMessage());
        verifyNoInteractions(tarifario);
    }

    @Test
    void rechazaUnGrupoVacio() {
        assertThrows(ReglaDominioException.class, () -> cotizador.cotizar(apt, estancia, List.of()));
    }

    @Test
    void cadaNocheTomaLaTarifaDeSuTemporadaRN05() {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));
        when(tarifario.tarifaDe(apt, LocalDate.of(2026, 12, 14))).thenReturn(new Tarifa("BASE", Dinero.de(50_000)));
        when(tarifario.tarifaDe(apt, LocalDate.of(2026, 12, 15))).thenReturn(new Tarifa("ALTA", Dinero.de(90_000)));
        when(tarifario.tarifaDe(apt, LocalDate.of(2026, 12, 16))).thenReturn(new Tarifa("ALTA", Dinero.de(90_000)));

        ValorCongelado valor = cotizador.cotizar(apt, estancia, List.of(adulto1));

        assertEquals(List.of("BASE", "ALTA", "ALTA"), valor.detalle().stream().map(CargoNoche::temporada).toList());
        assertEquals(Dinero.de(230_000), valor.total());
    }

    @Test
    void laNocheDeLaFechaDeSalidaNoSeCobraRN05() {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));
        when(tarifario.tarifaDe(eq(apt), any())).thenReturn(new Tarifa("BASE", Dinero.de(50_000)));

        ValorCongelado valor = cotizador.cotizar(apt, estancia, List.of(adulto1));

        assertEquals(3, valor.noches());
        assertTrue(valor.detalle().stream().noneMatch(c -> c.noche().equals(LocalDate.of(2026, 12, 17))));
    }

    @Test
    void rechazaCotizarSiFaltaLaTarifaDeUnaNoche() {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));
        when(tarifario.tarifaDe(eq(apt), any())).thenReturn(null);

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> cotizador.cotizar(apt, estancia, List.of(adulto1)));

        assertTrue(ex.getMessage().startsWith("El apartamento no tiene tarifa definida para la noche del"));
    }
}
