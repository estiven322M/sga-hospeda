package co.edu.uniquindio.sga.infrastructure.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import co.edu.uniquindio.sga.domain.exception.NotificacionFallidaException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoNotificacion;

/**
 * Adaptadores del puerto Notificador (I-01 a I-08). Ninguna prueba toca un servidor de correo real: el cliente SMTP es un
 * simulado (I-08) y el adaptador local no sale a ningún lado (I-02).
 */
class NotificadoresTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final CorreoElectronico DESTINO = new CorreoElectronico("laura@correo.com");

    private final BandejaEnMemoria bandeja = new BandejaEnMemoria();

    private static Notificacion reserva() {
        return new Notificacion(TipoNotificacion.RESERVA_CREADA, DESTINO, "Reserva RES-2026-00001 registrada",
                "Registramos su reserva", new CodigoReserva("RES-2026-00001"), AHORA);
    }

    private static Notificacion codigo() {
        return new Notificacion(TipoNotificacion.CODIGO_RECUPERACION, DESTINO, "Código", "Su código es 123456", null, AHORA);
    }

    // ---------- bandeja ----------

    @Test
    void laBandejaListaDeLaMasRecienteALaMasAntiguaDeDiezEnDiez() {
        for (int i = 0; i < 12; i++) {
            bandeja.registrar(new RegistroNotificacion(TipoNotificacion.RESERVA_CREADA, DESTINO, "asunto " + i, "m", null,
                    EstadoNotificacion.ENVIADA, null, AHORA.plusMinutes(i)));
        }

        Pagina<RegistroNotificacion> primera = bandeja.listar(SolicitudPagina.primera());
        Pagina<RegistroNotificacion> segunda = bandeja.listar(new SolicitudPagina(1));

        assertEquals(12, primera.totalElementos());
        assertEquals(10, primera.contenido().size());
        assertEquals("asunto 11", primera.contenido().get(0).asunto());
        assertEquals(2, segunda.contenido().size());
        assertEquals("asunto 0", segunda.contenido().get(1).asunto());
    }

    @Test
    void laBandejaConservaSoloLosUltimosAvisos() {
        for (int i = 0; i < BandejaEnMemoria.CAPACIDAD + 25; i++) {
            bandeja.registrar(new RegistroNotificacion(TipoNotificacion.RESERVA_CREADA, DESTINO, "a" + i, "m", null,
                    EstadoNotificacion.ENVIADA, null, AHORA));
        }

        assertEquals(BandejaEnMemoria.CAPACIDAD, bandeja.listar(SolicitudPagina.primera()).totalElementos());
        assertEquals("a" + (BandejaEnMemoria.CAPACIDAD + 24), bandeja.listar(SolicitudPagina.primera()).contenido().get(0).asunto());
    }

    // ---------- adaptador local ----------

    @Test
    void elAdaptadorLocalDejaElAvisoCompletoEnLaBandejaSinSalirAInternet() {
        new NotificadorLocal(bandeja, RELOJ).enviar(reserva());

        RegistroNotificacion registro = bandeja.listar(SolicitudPagina.primera()).contenido().get(0);
        assertEquals(EstadoNotificacion.ENVIADA, registro.estado());
        assertEquals(DESTINO, registro.destinatario());
        assertEquals("Registramos su reserva", registro.mensaje());
        assertEquals("RES-2026-00001", registro.codigoReserva().valor());
        assertEquals(AHORA, registro.registradaEn());
        assertNull(registro.detalle());
    }

    @Test
    void enModoLocalLaBandejaMuestraElCodigoDeRecuperacionPorqueEsElBuzonDeDemostracion() {
        new NotificadorLocal(bandeja, RELOJ).enviar(codigo());

        assertEquals("Su código es 123456", bandeja.listar(SolicitudPagina.primera()).contenido().get(0).mensaje());
    }

    // ---------- adaptador SMTP (con el cliente de correo simulado) ----------

    @Test
    void elAdaptadorSmtpArmaElCorreoYDejaConstanciaDeQueSeEnvio() {
        JavaMailSender cliente = mock(JavaMailSender.class);

        new NotificadorCorreoSmtp(cliente, bandeja, RELOJ, "sga@gmail.com").enviar(reserva());

        ArgumentCaptor<SimpleMailMessage> correo = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(cliente).send(correo.capture());
        assertEquals("sga@gmail.com", correo.getValue().getFrom());
        assertEquals("laura@correo.com", correo.getValue().getTo()[0]);
        assertEquals("Reserva RES-2026-00001 registrada", correo.getValue().getSubject());
        assertEquals("Registramos su reserva", correo.getValue().getText());
        assertEquals(EstadoNotificacion.ENVIADA, bandeja.listar(SolicitudPagina.primera()).contenido().get(0).estado());
    }

    @Test
    void conElProveedorRealElCodigoViajaEnElCorreoPeroNoQuedaEnLaBandeja() {
        JavaMailSender cliente = mock(JavaMailSender.class);

        new NotificadorCorreoSmtp(cliente, bandeja, RELOJ, "").enviar(codigo());

        ArgumentCaptor<SimpleMailMessage> correo = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(cliente).send(correo.capture());
        assertTrue(correo.getValue().getText().contains("123456"));
        assertEquals(Notificacion.CONTENIDO_OMITIDO, bandeja.listar(SolicitudPagina.primera()).contenido().get(0).mensaje());
    }

    @Test
    void siElServidorDeCorreoFallaQuedaFallidaConSuCausaYSeLanzaLaExcepcionDelPuerto() {
        JavaMailSender cliente = mock(JavaMailSender.class);
        doThrow(new MailSendException("Read timed out")).when(cliente).send(any(SimpleMailMessage.class));

        NotificacionFallidaException e = assertThrows(NotificacionFallidaException.class,
                () -> new NotificadorCorreoSmtp(cliente, bandeja, RELOJ, "sga@gmail.com").enviar(reserva()));

        assertTrue(e.getCause() instanceof MailSendException);
        RegistroNotificacion registro = bandeja.listar(SolicitudPagina.primera()).contenido().get(0);
        assertEquals(EstadoNotificacion.FALLIDA, registro.estado());
        assertTrue(registro.detalle().contains("MailSendException"));
        assertEquals(1, bandeja.listar(SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void unFalloConUnAvisoSensibleTampocoDejaElCodigoEnLaBandeja() {
        JavaMailSender cliente = mock(JavaMailSender.class);
        doThrow(new MailSendException("auth")).when(cliente).send(any(SimpleMailMessage.class));

        assertThrows(NotificacionFallidaException.class,
                () -> new NotificadorCorreoSmtp(cliente, bandeja, RELOJ, "").enviar(codigo()));

        assertEquals(Notificacion.CONTENIDO_OMITIDO, bandeja.listar(SolicitudPagina.primera()).contenido().get(0).mensaje());
    }
}
