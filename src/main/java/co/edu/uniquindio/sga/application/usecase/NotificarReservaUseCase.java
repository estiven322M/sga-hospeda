package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.application.security.DirectorioCorreos;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ContactoReservaRepository;
import co.edu.uniquindio.sga.domain.service.RedactorNotificaciones;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;

/**
 * Avisa al titular los tres momentos de su reserva: creada, confirmada y cancelada (10.3.2-A). Se llama DESPUÉS de que la
 * operación de negocio se confirmó (el controller lo hace al volver del caso de uso) y no lanza nunca: pase lo que pase
 * aquí, la reserva ya está hecha (I-05).
 * A quién se escribe: el correo de la cuenta del titular (mismo documento); si no tiene cuenta, el correo opcional que se
 * dio al crear la reserva; si no hay ninguno, el aviso queda registrado como SIN_CORREO.
 */
@Service
public class NotificarReservaUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificarReservaUseCase.class);

    private final RedactorNotificaciones redactor;
    private final ContactoReservaRepository contactos;
    private final DirectorioCorreos directorio;
    private final EmitirNotificacionUseCase emitir;

    public NotificarReservaUseCase(RedactorNotificaciones redactor, ContactoReservaRepository contactos,
                                   DirectorioCorreos directorio, EmitirNotificacionUseCase emitir) {
        this.redactor = redactor;
        this.contactos = contactos;
        this.directorio = directorio;
        this.emitir = emitir;
    }

    /** @param correoContacto correo opcional dado al reservar (puede ser nulo, vacío o mal escrito: se ignora) */
    public void reservaCreada(Reserva reserva, String correoContacto, LocalDateTime ahora) {
        try {
            if (correoContacto != null && !correoContacto.isBlank()) {
                contactos.guardar(reserva.getCodigo(), new CorreoElectronico(correoContacto));
            }
        } catch (RuntimeException e) {
            log.warn("No se guardó el correo de contacto de la reserva {}: {}", reserva.getCodigo().valor(), e.getMessage());
        }
        notificar(reserva, destinatario -> redactor.reservaCreada(reserva, destinatario, ahora));
    }

    public void reservaConfirmada(Reserva reserva, LocalDateTime ahora) {
        notificar(reserva, destinatario -> redactor.reservaConfirmada(reserva, destinatario, ahora));
    }

    public void reservaCancelada(Reserva reserva, LocalDateTime ahora) {
        notificar(reserva, destinatario -> redactor.reservaCancelada(reserva, destinatario, ahora));
    }

    private void notificar(Reserva reserva, Function<CorreoElectronico, Notificacion> redactar) {
        try {
            CorreoElectronico destinatario = directorio.correoDeCuenta(reserva.getTitular().getDocumento())
                    .or(() -> contactos.buscar(reserva.getCodigo()))
                    .orElse(null);
            emitir.ejecutar(redactar.apply(destinatario));
        } catch (RuntimeException e) {
            log.warn("No se notificó la reserva {}; la operación continúa: {}", reserva.getCodigo().valor(), e.getMessage());
        }
    }
}
