package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** RN-20 / definición 3.3: el tiempo de preparación se compara con la ventana salida → entrada. */
class TiempoPreparacionTest {

    private static final LocalTime SALIDA = LocalTime.of(11, 0);
    private static final LocalTime ENTRADA = LocalTime.of(15, 0); // ventana de 4 horas

    @Test
    void permiteEntradaElMismoDiaSiLaPreparacionCabeEnLaVentana() {
        assertTrue(new TiempoPreparacion(3).permiteEntradaElMismoDia(SALIDA, ENTRADA));
    }

    @Test
    void permiteEntradaElMismoDiaCuandoLaPreparacionIgualaLaVentana() {
        assertTrue(new TiempoPreparacion(4).permiteEntradaElMismoDia(SALIDA, ENTRADA));
    }

    @Test
    void rechazaEntradaElMismoDiaSiLaPreparacionExcedeLaVentana() {
        assertFalse(new TiempoPreparacion(5).permiteEntradaElMismoDia(SALIDA, ENTRADA));
    }

    @Test
    void sinTiempoDePreparacionSiempreSePermite() {
        assertTrue(new TiempoPreparacion(0).permiteEntradaElMismoDia(SALIDA, ENTRADA));
        assertTrue(new TiempoPreparacion(0).permiteEntradaElMismoDia(ENTRADA, SALIDA));
    }

    @Test
    void conVentanaNegativaSoloSePermiteSinPreparacion() {
        assertFalse(new TiempoPreparacion(1).permiteEntradaElMismoDia(ENTRADA, SALIDA));
    }

    @Test
    void exigeLasHorasDeSalidaYEntrada() {
        assertThrows(ReglaDominioException.class,
                () -> new TiempoPreparacion(1).permiteEntradaElMismoDia(null, ENTRADA));
    }

    @Test
    void noAdmiteHorasNegativas() {
        assertThrows(ReglaDominioException.class, () -> new TiempoPreparacion(-1));
    }
}
