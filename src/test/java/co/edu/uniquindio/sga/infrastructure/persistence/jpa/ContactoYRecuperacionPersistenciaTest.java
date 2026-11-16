package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.sga.domain.entity.CodigoRecuperacion;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;

/** Persistir no cambia el dominio: el correo de contacto de una reserva y el código de recuperación viajan y vuelven iguales. */
@DataJpaTest
@Import({ContactoReservaJpaRepository.class, CodigoRecuperacionJpaRepository.class})
class ContactoYRecuperacionPersistenciaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final CorreoElectronico CORREO = new CorreoElectronico("laura@correo.com");

    @Autowired private ContactoReservaJpaRepository contactos;
    @Autowired private CodigoRecuperacionJpaRepository codigos;

    // ---------- contacto de la reserva ----------

    @Test
    void sinContactoGuardadoNoHayCorreo() {
        assertTrue(contactos.buscar(new CodigoReserva("RES-2026-00001")).isEmpty());
    }

    @Test
    void guardaYRecuperaElCorreoDeContactoDeUnaReserva() {
        contactos.guardar(new CodigoReserva("RES-2026-00001"), CORREO);

        assertEquals(CORREO, contactos.buscar(new CodigoReserva("RES-2026-00001")).orElseThrow());
        assertTrue(contactos.buscar(new CodigoReserva("RES-2026-00002")).isEmpty());
    }

    @Test
    void guardarDeNuevoReemplazaElCorreoDeLaMismaReserva() {
        contactos.guardar(new CodigoReserva("RES-2026-00001"), CORREO);
        contactos.guardar(new CodigoReserva("RES-2026-00001"), new CorreoElectronico("otro@correo.com"));

        assertEquals("otro@correo.com", contactos.buscar(new CodigoReserva("RES-2026-00001")).orElseThrow().valor());
    }

    // ---------- código de recuperación ----------

    @Test
    void sinCodigoEmitidoNoHayNada() {
        assertTrue(codigos.buscarPorCorreo(CORREO).isEmpty());
    }

    @Test
    void guardaYRecuperaElCodigoConSuHashSuVigenciaYSuEstado() {
        codigos.guardar(CodigoRecuperacion.emitir(CORREO, "hash-bcrypt", AHORA, 15));

        CodigoRecuperacion leido = codigos.buscarPorCorreo(CORREO).orElseThrow();

        assertEquals("hash-bcrypt", leido.getHash());
        assertEquals(AHORA.plusMinutes(15), leido.getExpiraEn());
        assertFalse(leido.isUsado());
        assertEquals(0, leido.getIntentosFallidos());
    }

    @Test
    void elUsoYLosIntentosFallidosSeConservan() {
        CodigoRecuperacion codigo = CodigoRecuperacion.emitir(CORREO, "hash", AHORA, 15);
        try {
            codigo.consumir(false, AHORA.plusMinutes(1), 5);
        } catch (RuntimeException esperado) {
            // el intento fallido ya quedó sumado en el objeto
        }
        codigos.guardar(codigo);
        assertEquals(1, codigos.buscarPorCorreo(CORREO).orElseThrow().getIntentosFallidos());

        codigo.consumir(true, AHORA.plusMinutes(2), 5);
        codigos.guardar(codigo);

        CodigoRecuperacion leido = codigos.buscarPorCorreo(CORREO).orElseThrow();
        assertTrue(leido.isUsado());
        assertEquals(1, leido.getIntentosFallidos());
    }

    @Test
    void unCodigoNuevoReemplazaAlAnteriorDelMismoCorreo() {
        codigos.guardar(CodigoRecuperacion.emitir(CORREO, "hash-viejo", AHORA, 15));
        codigos.guardar(CodigoRecuperacion.emitir(CORREO, "hash-nuevo", AHORA.plusMinutes(5), 15));

        CodigoRecuperacion leido = codigos.buscarPorCorreo(CORREO).orElseThrow();
        assertEquals("hash-nuevo", leido.getHash());
        assertFalse(leido.isUsado());
    }
}
