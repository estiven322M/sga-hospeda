package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Regla 11: la baja es lógica, nunca física. */
class ApartamentoBajaLogicaTest {

    /** Un apartamento ya activo (los nuevos nacen inactivos, 7.4). */
    private Apartamento apartamento() {
        Apartamento apt = new Apartamento(new IdentificacionApartamento("APT-101"), "Vista", 2, 4);
        apt.activar();
        return apt;
    }

    @Test
    void darDeBajaLoDesactivaYActivarLoReactiva() {
        Apartamento apt = apartamento();
        apt.darDeBaja();
        assertFalse(apt.isActivo());
        apt.activar();
        assertTrue(apt.isActivo());
    }

    @Test
    void noSePuedeDarDeBajaDosVeces() {
        Apartamento apt = apartamento();
        apt.darDeBaja();
        assertThrows(ReglaDominioException.class, apt::darDeBaja);
    }

    @Test
    void noSePuedeActivarUnApartamentoYaActivo() {
        assertThrows(ReglaDominioException.class, apartamento()::activar);
    }
}
