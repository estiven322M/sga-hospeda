package co.edu.uniquindio.sga.infrastructure.notificaciones;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.Notificador;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/**
 * Adaptador LOCAL del puerto Notificador (I-02): no sale a ningún lado. Registra el aviso completo en el log y en la
 * bandeja en memoria, así el sistema funciona y se demuestra sin internet ni credenciales. Es el que viene activo por
 * defecto ({@code sga.notificaciones.proveedor=local}); como no hay servidor externo, no tiene tiempo de espera ni falla.
 * Deja visible el contenido completo (incluido el código de recuperación) porque en este modo la bandeja ES el buzón
 * de demostración; con el proveedor real ese contenido nunca se guarda.
 */
@Component
@ConditionalOnProperty(name = "sga.notificaciones.proveedor", havingValue = "local", matchIfMissing = true)
public class NotificadorLocal implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorLocal.class);

    private final BandejaNotificaciones bandeja;
    private final Clock reloj;

    public NotificadorLocal(BandejaNotificaciones bandeja, Clock reloj) {
        this.bandeja = bandeja;
        this.reloj = reloj;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        log.info("[Notificación local] {} para {}: {}", notificacion.tipo(), notificacion.destinatario().valor(),
                notificacion.asunto());
        bandeja.registrar(RegistroNotificacion.de(notificacion, notificacion.mensaje(), EstadoNotificacion.ENVIADA,
                null, LocalDateTime.now(reloj)));
    }
}
