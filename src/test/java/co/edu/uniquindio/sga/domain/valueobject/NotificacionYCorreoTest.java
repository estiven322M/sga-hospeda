package co.edu.uniquindio.sga.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** Objetos de valor de las notificaciones (10.3.2-A): el correo y el aviso, validados en su constructor. */
class NotificacionYCorreoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Test
    void elCorreoSeNormalizaAMinusculasYSinEspacios() {
        assertEquals("laura@sga.local", new CorreoElectronico("  Laura@SGA.local ").valor());
        assertEquals(new CorreoElectronico("A@b.co"), new CorreoElectronico("a@B.CO"));
    }

    @Test
    void elCorreoVacioONuloSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> new CorreoElectronico(null));
        assertThrows(ReglaDominioException.class, () -> new CorreoElectronico("   "));
    }

    @Test
    void elCorreoMalFormadoSeRechaza() {
        for (String malo : new String[] {"sin-arroba", "a@b", "a@@b.co", "a b@c.co", "@c.co", "a@.co "}) {
            ReglaDominioException e = assertThrows(ReglaDominioException.class, () -> new CorreoElectronico(malo), malo);
            assertEquals("El correo electrónico no tiene un formato válido", e.getMessage());
        }
    }

    @Test
    void elCorreoMuyLargoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> new CorreoElectronico("a".repeat(120) + "@b.co"));
    }

    @Test
    void soloElCodigoDeRecuperacionEsUnAvisoSensible() {
        assertTrue(TipoNotificacion.CODIGO_RECUPERACION.esSensible());
        assertFalse(TipoNotificacion.RESERVA_CREADA.esSensible());
        assertFalse(TipoNotificacion.RESERVA_CONFIRMADA.esSensible());
        assertFalse(TipoNotificacion.RESERVA_CANCELADA.esSensible());
    }

    @Test
    void unAvisoSensibleNoDejaSuContenidoEnElRegistro() {
        Notificacion codigo = new Notificacion(TipoNotificacion.CODIGO_RECUPERACION, new CorreoElectronico("a@b.co"),
                "Código", "Su código es 123456", null, AHORA);
        Notificacion reserva = new Notificacion(TipoNotificacion.RESERVA_CREADA, new CorreoElectronico("a@b.co"),
                "Reserva", "Registramos su reserva", new CodigoReserva("RES-2026-00001"), AHORA);

        assertEquals(Notificacion.CONTENIDO_OMITIDO, codigo.mensajeRegistrable());
        assertEquals("Registramos su reserva", reserva.mensajeRegistrable());
    }

    @Test
    void elAvisoExigeTipoAsuntoMensajeYFecha() {
        CorreoElectronico a = new CorreoElectronico("a@b.co");
        assertThrows(ReglaDominioException.class, () -> new Notificacion(null, a, "x", "y", null, AHORA));
        assertThrows(ReglaDominioException.class,
                () -> new Notificacion(TipoNotificacion.RESERVA_CREADA, a, " ", "y", null, AHORA));
        assertThrows(ReglaDominioException.class,
                () -> new Notificacion(TipoNotificacion.RESERVA_CREADA, a, "x", "", null, AHORA));
        assertThrows(ReglaDominioException.class,
                () -> new Notificacion(TipoNotificacion.RESERVA_CREADA, a, "x", "y", null, null));
    }

    @Test
    void elAvisoPuedeNoTenerDestinatario() {
        Notificacion sinCorreo = new Notificacion(TipoNotificacion.RESERVA_CREADA, null, "x", "y", null, AHORA);

        assertEquals(null, sinCorreo.destinatario());
    }

    @Test
    void elRegistroExigeTipoEstadoYFecha() {
        assertThrows(ReglaDominioException.class,
                () -> new RegistroNotificacion(null, null, "a", "m", null, EstadoNotificacion.ENVIADA, null, AHORA));
        assertThrows(ReglaDominioException.class,
                () -> new RegistroNotificacion(TipoNotificacion.RESERVA_CREADA, null, "a", "m", null, null, null, AHORA));
        assertThrows(ReglaDominioException.class,
                () -> new RegistroNotificacion(TipoNotificacion.RESERVA_CREADA, null, "a", "m", null,
                        EstadoNotificacion.ENVIADA, null, null));
    }
}
