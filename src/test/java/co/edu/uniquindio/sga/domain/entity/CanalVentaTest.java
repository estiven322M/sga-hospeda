package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/** 7.8: el canal de venta (código, nombre, tipo, credencial con hash, baja lógica) y su parte en "canal + identificador externo" (RN-19). */
class CanalVentaTest {

    private static CanalVenta canal(String codigo) {
        return new CanalVenta(codigo, "Booking de prueba", TipoCanalVenta.PLATAFORMA_EN_LINEA, CredencialCanal.de("sgc_secreta").hash());
    }

    @Test
    void naceActivoYConElCodigoEnMayusculas() {
        CanalVenta canal = canal(" demo ");

        assertEquals("DEMO", canal.getCodigo());
        assertTrue(canal.isActivo());
        assertEquals("Booking de prueba", canal.getNombre());
        assertEquals(TipoCanalVenta.PLATAFORMA_EN_LINEA, canal.getTipo());
    }

    @Test
    void elCodigoDebeTenerDosADoceCaracteresValidos() {
        for (String malo : new String[] {null, "", "A", "ABCDEFGHIJKLM", "con espacio", "ñandú", "_AB", "A:B"}) {
            assertThrows(ReglaDominioException.class, () -> canal(malo), "código: " + malo);
        }
        canal("AB");
        canal("ABCDEFGHIJKL");
        canal("A-B_C9");
    }

    @Test
    void exigeNombreTipoYCredencial() {
        String hash = CredencialCanal.de("x").hash();
        assertThrows(ReglaDominioException.class, () -> new CanalVenta("DEMO", " ", TipoCanalVenta.OTRO, hash));
        assertThrows(ReglaDominioException.class, () -> new CanalVenta("DEMO", "n".repeat(81), TipoCanalVenta.OTRO, hash));
        assertThrows(ReglaDominioException.class, () -> new CanalVenta("DEMO", "Nombre", null, hash));
        assertThrows(ReglaDominioException.class, () -> new CanalVenta("DEMO", "Nombre", TipoCanalVenta.OTRO, " "));
    }

    @Test
    void desactivarEsLogicoYNoSePuedeHacerDosVeces() {
        CanalVenta canal = canal("DEMO");

        canal.desactivar();

        assertFalse(canal.isActivo());
        ReglaDominioException e = assertThrows(ReglaDominioException.class, canal::desactivar);
        assertEquals("El canal DEMO ya está desactivado", e.getMessage());
    }

    @Test
    void sePuedeVolverAActivarPeroNoUnoActivo() {
        CanalVenta canal = canal("DEMO");
        canal.desactivar();

        canal.activar();

        assertTrue(canal.isActivo());
        assertThrows(ReglaDominioException.class, canal::activar);
    }

    @Test
    void soloUnCanalActivoAceptaSuCredencial() {
        CanalVenta canal = canal("DEMO");
        String hashBueno = CredencialCanal.de("sgc_secreta").hash();

        assertTrue(canal.aceptaCredencial(hashBueno));
        assertFalse(canal.aceptaCredencial(CredencialCanal.de("otra").hash()));
        canal.desactivar();
        assertFalse(canal.aceptaCredencial(hashBueno), "un canal desactivado ya no acepta su credencial");
    }

    @Test
    void elIdentificadorSeGuardaConElCodigoDelCanalPorDelante() {
        IdentificadorExterno guardado = canal("DEMO").identificadorPara("  RES-9001 ");

        assertEquals("DEMO:RES-9001", guardado.valor());
    }

    @Test
    void dosCanalesPuedenUsarElMismoIdentificadorSinChocar() {
        assertNotEquals(canal("AAA").identificadorPara("RES-1"), canal("BBB").identificadorPara("RES-1"));
    }

    @Test
    void elIdentificadorPropioCabeEnLosSesentaCaracteresMenosElCodigo() {
        CanalVenta canal = canal("DEMO");   // 4 + ":" => 55 para el canal

        assertEquals(55, canal.maximoIdentificadorPropio());
        canal.identificadorPara("x".repeat(55));
        ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> canal.identificadorPara("x".repeat(56)));
        assertEquals("El identificador externo de este canal no puede superar 55 caracteres", e.getMessage());
        assertThrows(ReglaDominioException.class, () -> canal.identificadorPara(" "));
        assertThrows(ReglaDominioException.class, () -> canal.identificadorPara(null));
    }

    @Test
    void elIdentificadorPropioSeRecuperaSinElCodigo() {
        CanalVenta canal = canal("DEMO");

        assertEquals("RES-9001", canal.identificadorPropio(canal.identificadorPara("RES-9001")));
        assertEquals("EXT-1", canal.identificadorPropio(new IdentificadorExterno("EXT-1")), "uno sin código se devuelve tal cual");
    }

    @Test
    void unCanalSoloReconoceSusPropiasReservas() {
        CanalVenta demo = canal("DEMO");
        CanalVenta otro = canal("OTRO");
        IdentificadorExterno deDemo = demo.identificadorPara("RES-1");

        assertTrue(demo.esDeEsteCanal(deDemo));
        assertFalse(otro.esDeEsteCanal(deDemo));
        assertFalse(demo.esDeEsteCanal(new IdentificadorExterno("EXT-1")));
        assertFalse(demo.esDeEsteCanal(null));
    }

    @Test
    void laIgualdadEsPorCodigo() {
        CanalVenta uno = canal("DEMO");
        CanalVenta mismoCodigoOtroNombre = new CanalVenta("demo", "Otro nombre", TipoCanalVenta.OTRO, CredencialCanal.de("y").hash());

        assertEquals(uno, mismoCodigoOtroNombre);
        assertEquals(uno.hashCode(), mismoCodigoOtroNombre.hashCode());
        assertNotEquals(uno, canal("OTRO"));
    }

    @Test
    void reconstruirRestauraElEstadoDeActividad() {
        CanalVenta inactivo = CanalVenta.reconstruir("DEMO", "Nombre", TipoCanalVenta.AGENCIA_DE_VIAJES, "hash", false);

        assertFalse(inactivo.isActivo());
        assertEquals("hash", inactivo.getCredencialHash());
    }
}
