package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Random;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;

/** 7.2: el código de recuperación es de 6 dígitos, vale 15 minutos, sirve una sola vez y tiene tope de intentos. */
class CodigoRecuperacionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final CorreoElectronico CORREO = new CorreoElectronico("laura@correo.com");
    private static final int MAXIMO = 5;

    private CodigoRecuperacion emitido() {
        return CodigoRecuperacion.emitir(CORREO, "hash-bcrypt", AHORA, 15);
    }

    @Test
    void unCodigoEmitidoVenceALosQuinceMinutosYNaceSinUsarYSinIntentos() {
        CodigoRecuperacion codigo = emitido();

        assertEquals(AHORA.plusMinutes(15), codigo.getExpiraEn());
        assertFalse(codigo.isUsado());
        assertEquals(0, codigo.getIntentosFallidos());
    }

    @Test
    void sirveDentroDeLaVigenciaYQuedaUsado() {
        CodigoRecuperacion codigo = emitido();

        codigo.consumir(true, AHORA.plusMinutes(14), MAXIMO);

        assertTrue(codigo.isUsado());
    }

    @Test
    void esDeUnSoloUsoElSegundoIntentoConElMismoCodigoSeRechaza() {
        CodigoRecuperacion codigo = emitido();
        codigo.consumir(true, AHORA.plusMinutes(1), MAXIMO);

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> codigo.consumir(true, AHORA.plusMinutes(2), MAXIMO));

        assertEquals(CodigoRecuperacion.MENSAJE_RECHAZO, e.getMessage());
    }

    @Test
    void noSirveDespuesDeLosQuinceMinutos() {
        CodigoRecuperacion codigo = emitido();

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> codigo.consumir(true, AHORA.plusMinutes(15), MAXIMO));

        assertEquals(CodigoRecuperacion.MENSAJE_RECHAZO, e.getMessage());
        assertFalse(codigo.isUsado());
    }

    @Test
    void unCodigoQueNoCoincideSeRechazaYSumaUnIntentoFallido() {
        CodigoRecuperacion codigo = emitido();

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> codigo.consumir(false, AHORA.plusMinutes(1), MAXIMO));

        assertEquals(CodigoRecuperacion.MENSAJE_RECHAZO, e.getMessage());
        assertEquals(1, codigo.getIntentosFallidos());
        assertFalse(codigo.isUsado());
    }

    @Test
    void conLosIntentosAgotadosNiElCodigoCorrectoSirve() {
        CodigoRecuperacion codigo = emitido();
        for (int i = 0; i < MAXIMO; i++) {
            assertThrows(ReglaDominioException.class, () -> codigo.consumir(false, AHORA.plusMinutes(1), MAXIMO));
        }

        assertThrows(ReglaDominioException.class, () -> codigo.consumir(true, AHORA.plusMinutes(2), MAXIMO));
        assertFalse(codigo.isUsado());
    }

    @Test
    void todosLosRechazosDicenLoMismoParaNoRevelarElMotivo() {
        CodigoRecuperacion vencido = emitido();
        CodigoRecuperacion usado = emitido();
        usado.consumir(true, AHORA.plusMinutes(1), MAXIMO);
        CodigoRecuperacion equivocado = emitido();

        String porVencido = assertThrows(ReglaDominioException.class,
                () -> vencido.consumir(true, AHORA.plusHours(1), MAXIMO)).getMessage();
        String porUsado = assertThrows(ReglaDominioException.class,
                () -> usado.consumir(true, AHORA.plusMinutes(2), MAXIMO)).getMessage();
        String porEquivocado = assertThrows(ReglaDominioException.class,
                () -> equivocado.consumir(false, AHORA.plusMinutes(2), MAXIMO)).getMessage();

        assertEquals(porVencido, porUsado);
        assertEquals(porVencido, porEquivocado);
    }

    @Test
    void generaCodigosDeSeisDigitosConCerosALaIzquierda() {
        Random azar = new Random(7);
        for (int i = 0; i < 200; i++) {
            assertTrue(CodigoRecuperacion.tieneFormato(CodigoRecuperacion.generarTexto(azar)));
        }
        assertEquals("000042", CodigoRecuperacion.generarTexto(new Random() {
            @Override
            public int nextInt(int limite) {
                return 42;
            }
        }));
    }

    @Test
    void soloLosSeisDigitosTienenFormatoDeCodigo() {
        assertTrue(CodigoRecuperacion.tieneFormato("123456"));
        assertFalse(CodigoRecuperacion.tieneFormato("12345"));
        assertFalse(CodigoRecuperacion.tieneFormato("1234567"));
        assertFalse(CodigoRecuperacion.tieneFormato("12345a"));
        assertFalse(CodigoRecuperacion.tieneFormato(null));
    }

    @Test
    void exigeCorreoHashYVigenciaPositiva() {
        assertThrows(ReglaDominioException.class, () -> CodigoRecuperacion.emitir(null, "h", AHORA, 15));
        assertThrows(ReglaDominioException.class, () -> CodigoRecuperacion.emitir(CORREO, " ", AHORA, 15));
        assertThrows(ReglaDominioException.class, () -> CodigoRecuperacion.emitir(CORREO, "h", AHORA, 0));
    }

    @Test
    void reconstruirRestauraElEstadoGuardado() {
        CodigoRecuperacion codigo = CodigoRecuperacion.reconstruir(CORREO, "h", AHORA.plusMinutes(5), true, 3);

        assertTrue(codigo.isUsado());
        assertEquals(3, codigo.getIntentosFallidos());
        assertEquals(AHORA.plusMinutes(5), codigo.getExpiraEn());
    }
}
