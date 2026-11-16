package co.edu.uniquindio.sga.domain.service;

import java.util.Map;
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

    // ---------- RP-01 y RP-02 (L-20) ----------

    private static final Map<String, Integer> MINIMOS = Map.of("ALTA", 3, "MEDIA", 2);

    /** Calendario de 2028: ALTA del 15-jun al 15-jul, MEDIA del 30-abr al 4-may y BASE el resto. */
    private static String temporadaDe(LocalDate noche) {
        if (!noche.isBefore(LocalDate.of(2028, 6, 15)) && !noche.isAfter(LocalDate.of(2028, 7, 15))) {
            return "ALTA";
        }
        if (!noche.isBefore(LocalDate.of(2028, 4, 30)) && !noche.isAfter(LocalDate.of(2028, 5, 4))) {
            return "MEDIA";
        }
        return "BASE";
    }

    private void tarifasDe2028(long tarifa) {
        when(config.umbralEdadFacturable()).thenReturn(new UmbralEdadFacturable(12));
        when(tarifario.tarifaDe(eq(apt), any())).thenAnswer(inv ->
                new Tarifa(temporadaDe(inv.getArgument(1)), Dinero.de(tarifa)));
    }

    private ValorCongelado cotizar(LocalDate entrada, LocalDate salida) {
        return cotizador.cotizar(apt, new Estancia(entrada, salida), List.of(adulto1));
    }

    private void conRP01() {
        when(config.estanciaMinimaPorTemporada()).thenReturn(MINIMOS);
    }

    private void conRP02(int desde, int porcentaje) {
        when(config.nochesDescuentoEstanciaLarga()).thenReturn(desde);
        when(config.porcentajeDescuentoEstanciaLarga()).thenReturn(porcentaje);
    }

    @Test
    void altaConDosNochesSeRechazaRP01() {
        tarifasDe2028(100_000);
        conRP01();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> cotizar(LocalDate.of(2028, 7, 11), LocalDate.of(2028, 7, 13)));

        assertEquals("En temporada ALTA la estancia mínima es de 3 noches", ex.getMessage());
    }

    @Test
    void altaConUnaNocheSeRechazaRP01() {
        tarifasDe2028(100_000);
        conRP01();

        assertThrows(ReglaDominioException.class, () -> cotizar(LocalDate.of(2028, 7, 11), LocalDate.of(2028, 7, 12)));
    }

    @Test
    void altaConTresNochesSeCotizaRP01() {
        tarifasDe2028(100_000);
        conRP01();

        ValorCongelado valor = cotizar(LocalDate.of(2028, 7, 11), LocalDate.of(2028, 7, 14));

        assertEquals(3, valor.noches());
        assertEquals(Dinero.de(300_000), valor.total());
    }

    @Test
    void mediaExigeDosNochesRP01() {
        tarifasDe2028(100_000);
        conRP01();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> cotizar(LocalDate.of(2028, 5, 1), LocalDate.of(2028, 5, 2)));
        assertEquals("En temporada MEDIA la estancia mínima es de 2 noches", ex.getMessage());
        assertEquals(2, cotizar(LocalDate.of(2028, 5, 1), LocalDate.of(2028, 5, 3)).noches());
    }

    @Test
    void laTemporadaBaseNoExigeMinimoRP01() {
        tarifasDe2028(100_000);
        conRP01();

        assertEquals(1, cotizar(LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 11)).noches());
    }

    @Test
    void unaEstanciaQueCruzaDeBaseAAltaConDosNochesSeRechazaPorLaNocheEnAltaRP01() {
        tarifasDe2028(100_000);
        conRP01();

        // 14-jun (BASE) y 15-jun (ALTA): 2 noches y una de ellas es ALTA
        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> cotizar(LocalDate.of(2028, 6, 14), LocalDate.of(2028, 6, 16)));
        assertEquals("En temporada ALTA la estancia mínima es de 3 noches", ex.getMessage());
    }

    @Test
    void unaEstanciaQueCruzaDeBaseAAltaConTresNochesSeCotizaRP01() {
        tarifasDe2028(100_000);
        conRP01();

        ValorCongelado valor = cotizar(LocalDate.of(2028, 6, 14), LocalDate.of(2028, 6, 17));

        assertEquals(List.of("BASE", "ALTA", "ALTA"), valor.detalle().stream().map(CargoNoche::temporada).toList());
    }

    @Test
    void sinEstanciasMinimasConfiguradasNingunaTemporadaRestringeRP01() {
        tarifasDe2028(100_000); // el mock devuelve un mapa vacío: ninguna temporada exige mínimo

        assertEquals(1, cotizar(LocalDate.of(2028, 7, 11), LocalDate.of(2028, 7, 12)).noches());
    }

    @Test
    void conSeisNochesNoHayDescuentoRP02() {
        tarifasDe2028(100_000);
        conRP02(7, 10);

        ValorCongelado valor = cotizar(LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 16));

        assertEquals(6, valor.noches());
        assertEquals(0, valor.porcentajeDescuento());
        assertEquals(Dinero.de(600_000), valor.bruto());
        assertEquals(Dinero.CERO, valor.descuento());
        assertEquals(Dinero.de(600_000), valor.total());
    }

    @Test
    void conSieteNochesSeDescuentaElDiezPorCientoRP02() {
        tarifasDe2028(100_000);
        conRP02(7, 10);

        ValorCongelado valor = cotizar(LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 17));

        assertEquals(7, valor.noches());
        assertEquals(10, valor.porcentajeDescuento());
        assertEquals(Dinero.de(700_000), valor.bruto());
        assertEquals(Dinero.de(70_000), valor.descuento());
        assertEquals(Dinero.de(630_000), valor.total());
    }

    @Test
    void elDescuentoSeAplicaUnaSolaVezSobreElTotalRP02() {
        tarifasDe2028(50_005);
        conRP02(7, 10);

        ValorCongelado valor = cotizar(LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 17));

        assertEquals(Dinero.de(350_035), valor.bruto());
        assertEquals(Dinero.de(35_004), valor.descuento()); // 35.003,5 redondeado una vez
        assertEquals(Dinero.de(315_031), valor.total());
    }

    @Test
    void elDescuentoCuentaTodosLosOcupantesFacturablesRP02() {
        tarifasDe2028(100_000);
        conRP02(7, 10);

        ValorCongelado valor = cotizador.cotizar(apt,
                new Estancia(LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 17)), List.of(adulto1, adulto2, nino));

        assertEquals(Dinero.de(1_400_000), valor.bruto());
        assertEquals(Dinero.de(1_260_000), valor.total());
    }

    @Test
    void conUnPorcentajeDeCeroElDescuentoQuedaDesactivadoRP02() {
        tarifasDe2028(100_000);
        conRP02(7, 0);

        ValorCongelado valor = cotizar(LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 20)); // 10 noches

        assertEquals(0, valor.porcentajeDescuento());
        assertEquals(Dinero.de(1_000_000), valor.total());
    }

    @Test
    void elMinimoDeNochesYElDescuentoConvivenEnUnaEstanciaLargaEnAltaRP01RP02() {
        tarifasDe2028(100_000);
        conRP01();
        conRP02(7, 10);

        ValorCongelado valor = cotizar(LocalDate.of(2028, 6, 20), LocalDate.of(2028, 6, 28)); // 8 noches en ALTA

        assertEquals(8, valor.noches());
        assertEquals(Dinero.de(720_000), valor.total()); // 800.000 - 10 %
    }

    @Test
    void laEstanciaMinimaSeEvaluaDespuesDeLaFacturabilidadRN06() {
        tarifasDe2028(100_000);
        conRP01();

        // un niño de 5 años en una sola noche de ALTA: primero falla RN-06 (sin facturables), no RP-01
        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> cotizador.cotizar(apt,
                new Estancia(LocalDate.of(2028, 7, 11), LocalDate.of(2028, 7, 12)), List.of(nino)));
        assertEquals("La reserva debe tener al menos un ocupante facturable", ex.getMessage());
    }
}
