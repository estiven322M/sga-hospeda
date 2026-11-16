package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** RP-01 (L-20): estancia mínima por temporada. */
class EstanciaMinimaPorTemporadaTest {

    private final EstanciaMinimaPorTemporada reglas = new EstanciaMinimaPorTemporada(Map.of("ALTA", 3, "MEDIA", 2));

    /** Desglose con una temporada por noche, empezando el 10 de julio de 2028. */
    private static List<CargoNoche> noches(String... temporadas) {
        return IntStream.range(0, temporadas.length)
                .mapToObj(i -> new CargoNoche(LocalDate.of(2028, 7, 10).plusDays(i), temporadas[i], Dinero.de(100_000), 1))
                .toList();
    }

    @Test
    void unaEstanciaEnAltaConMenosNochesQueElMinimoSeRechazaConElMensajeDeLaRegla() {
        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> reglas.verificar(noches("ALTA", "ALTA")));

        assertEquals("En temporada ALTA la estancia mínima es de 3 noches", ex.getMessage());
    }

    @Test
    void unaNocheEnAltaTambienSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> reglas.verificar(noches("ALTA")));
    }

    @Test
    void conExactamenteElMinimoSePermiteEnAlta() {
        assertDoesNotThrow(() -> reglas.verificar(noches("ALTA", "ALTA", "ALTA")));
    }

    @Test
    void conMasDelMinimoSePermite() {
        assertDoesNotThrow(() -> reglas.verificar(noches("ALTA", "ALTA", "ALTA", "ALTA", "ALTA")));
    }

    @Test
    void enMediaElMinimoEsDeDosNoches() {
        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> reglas.verificar(noches("MEDIA")));
        assertEquals("En temporada MEDIA la estancia mínima es de 2 noches", ex.getMessage());
        assertDoesNotThrow(() -> reglas.verificar(noches("MEDIA", "MEDIA")));
    }

    @Test
    void laTemporadaBaseNoExigeMinimoPorqueNoFiguraEnLaConfiguracion() {
        assertDoesNotThrow(() -> reglas.verificar(noches("BASE")));
    }

    @Test
    void basta_con_que_una_sola_noche_caiga_en_la_temporada_para_exigir_su_minimo() {
        // BASE + ALTA = 2 noches, y una de ellas es ALTA (mínimo 3)
        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> reglas.verificar(noches("BASE", "ALTA")));
        assertEquals("En temporada ALTA la estancia mínima es de 3 noches", ex.getMessage());
        // con una noche más de BASE ya son 3
        assertDoesNotThrow(() -> reglas.verificar(noches("BASE", "BASE", "ALTA")));
    }

    @Test
    void cuandoVariasTemporadasTienenMinimoSeExigeElMayor() {
        // MEDIA (2) y ALTA (3) en 2 noches: falla por ALTA
        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> reglas.verificar(noches("MEDIA", "ALTA")));
        assertEquals("En temporada ALTA la estancia mínima es de 3 noches", ex.getMessage());
    }

    @Test
    void elNombreDeLaTemporadaSeComparaSinDistinguirMayusculas() {
        EstanciaMinimaPorTemporada enMinusculas = new EstanciaMinimaPorTemporada(Map.of("alta", 3));

        assertEquals(3, enMinusculas.minimoDe("ALTA"));
        assertThrows(ReglaDominioException.class, () -> enMinusculas.verificar(noches("ALTA")));
    }

    @Test
    void sinMinimosConfiguradosNingunaEstanciaSeRechaza() {
        assertDoesNotThrow(() -> EstanciaMinimaPorTemporada.SIN_MINIMOS.verificar(noches("ALTA")));
        assertDoesNotThrow(() -> new EstanciaMinimaPorTemporada(null).verificar(noches("ALTA")));
    }

    @Test
    void unMinimoMenorQueUnaNocheONombreEnBlancoNoEsValido() {
        assertThrows(ReglaDominioException.class, () -> new EstanciaMinimaPorTemporada(Map.of("ALTA", 0)));
        assertThrows(ReglaDominioException.class, () -> new EstanciaMinimaPorTemporada(Map.of("ALTA", -2)));
        assertThrows(ReglaDominioException.class, () -> new EstanciaMinimaPorTemporada(Map.of(" ", 3)));
    }

    @Test
    void dosClavesQueSoloSeDiferencianEnMayusculasSonUnaConfiguracionAmbigua() {
        Map<String, Integer> ambiguo = new HashMap<>();
        ambiguo.put("ALTA", 3);
        ambiguo.put("alta", 4);

        assertThrows(ReglaDominioException.class, () -> new EstanciaMinimaPorTemporada(ambiguo));
    }

    @Test
    void elMapaQueSeEntregaNoPuedeAlterarseDesdeFuera() {
        Map<String, Integer> original = new HashMap<>(Map.of("ALTA", 3));
        EstanciaMinimaPorTemporada regla = new EstanciaMinimaPorTemporada(original);

        original.put("ALTA", 1);

        assertEquals(3, regla.minimoDe("ALTA"));
    }
}
