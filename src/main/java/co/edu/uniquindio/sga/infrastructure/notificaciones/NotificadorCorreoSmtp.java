package co.edu.uniquindio.sga.infrastructure.notificaciones;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.exception.NotificacionFallidaException;
import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.Notificador;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/**
 * Adaptador REAL del puerto Notificador: envía por SMTP (Gmail con contraseña de aplicación, I-07). Se activa con
 * {@code sga.notificaciones.proveedor=smtp}.
 * <ul>
 *   <li>Tiempo de espera (I-03): {@code sga.notificaciones.tiempo-espera-ms} (5000) se aplica a la conexión, a la lectura y
 *       a la escritura del cliente SMTP (propiedades {@code spring.mail.properties.mail.smtp.*timeout}).</li>
 *   <li>Fallo (I-04): se deja FALLIDA con la causa en la bandeja (el usuario administrador lo ve en Notificaciones),
 *       y se lanza NotificacionFallidaException; quien notifica la captura y la operación de negocio continúa (I-05).</li>
 *   <li>Credenciales (I-06): usuario y contraseña de aplicación salen de application-secrets.properties o de variables
 *       de entorno, nunca del repositorio.</li>
 *   <li>El código de recuperación viaja en el correo, pero en la bandeja solo queda "(contenido sensible omitido)".</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "sga.notificaciones.proveedor", havingValue = "smtp")
public class NotificadorCorreoSmtp implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorCorreoSmtp.class);

    private final JavaMailSender correo;
    private final BandejaNotificaciones bandeja;
    private final Clock reloj;
    private final String remitente;

    public NotificadorCorreoSmtp(JavaMailSender correo, BandejaNotificaciones bandeja, Clock reloj,
                                 @Value("${sga.notificaciones.remitente:${spring.mail.username:}}") String remitente) {
        this.correo = correo;
        this.bandeja = bandeja;
        this.reloj = reloj;
        this.remitente = remitente;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (remitente != null && !remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(notificacion.destinatario().valor());
            mensaje.setSubject(notificacion.asunto());
            mensaje.setText(notificacion.mensaje());
            correo.send(mensaje);
        } catch (RuntimeException fallo) {
            // Se registra el motivo, sin datos sensibles: el mensaje de una excepción de correo no trae el contenido
            log.warn("Falló el envío SMTP de {} a {}: {}", notificacion.tipo(), notificacion.destinatario().valor(),
                    fallo.getMessage());
            bandeja.registrar(RegistroNotificacion.de(notificacion, notificacion.mensajeRegistrable(),
                    EstadoNotificacion.FALLIDA, "No se pudo enviar el correo: " + fallo.getClass().getSimpleName(),
                    LocalDateTime.now(reloj)));
            throw new NotificacionFallidaException("No se pudo enviar el correo", fallo);
        }
        bandeja.registrar(RegistroNotificacion.de(notificacion, notificacion.mensajeRegistrable(),
                EstadoNotificacion.ENVIADA, null, LocalDateTime.now(reloj)));
    }
}
