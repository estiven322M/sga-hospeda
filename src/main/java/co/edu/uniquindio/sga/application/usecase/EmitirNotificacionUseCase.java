package co.edu.uniquindio.sga.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.Notificador;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/**
 * Emite un aviso por el puerto Notificador de forma que su fallo NUNCA afecte al negocio (I-04, I-05).
 * <ul>
 *   <li>Sin destinatario: no se llama al proveedor; queda constancia SIN_CORREO en la bandeja y en el log.</li>
 *   <li>Con destinatario: se envía; si el proveedor falla, el adaptador deja FALLIDA en la bandeja y aquí se
 *       captura cualquier excepción y se registra en el log. No se relanza.</li>
 * </ul>
 * Deliberadamente NO es transaccional: se llama cuando la transacción del negocio ya se confirmó, así un correo lento
 * o caído no retiene la base de datos ni puede revertir la reserva.
 */
@Service
public class EmitirNotificacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(EmitirNotificacionUseCase.class);

    private final Notificador notificador;
    private final BandejaNotificaciones bandeja;
    private final Clock reloj;

    public EmitirNotificacionUseCase(Notificador notificador, BandejaNotificaciones bandeja, Clock reloj) {
        this.notificador = notificador;
        this.bandeja = bandeja;
        this.reloj = reloj;
    }

    /** No lanza nunca. */
    public void ejecutar(Notificacion notificacion) {
        try {
            if (notificacion.destinatario() == null) {
                log.warn("No se pudo notificar ({}{}): no hay un correo conocido para el titular",
                        notificacion.tipo(), notificacion.codigoReserva() == null ? ""
                                : " " + notificacion.codigoReserva().valor());
                bandeja.registrar(RegistroNotificacion.de(notificacion, notificacion.mensajeRegistrable(),
                        EstadoNotificacion.SIN_CORREO, "No hay un correo conocido para el titular",
                        LocalDateTime.now(reloj)));
                return;
            }
            notificador.enviar(notificacion);
        } catch (RuntimeException fallo) {
            log.warn("La notificación {} no pudo enviarse; la operación de negocio continúa: {}",
                    notificacion.tipo(), fallo.getMessage());
        }
    }
}
