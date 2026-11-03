package co.edu.uniquindio.sga.infrastructure.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.usecase.VencerReservasPendientesUseCase;

/**
 * Dispara periódicamente el vencimiento de reservas pendientes (RN-21), sin intervención manual.
 * Es solo el "reloj": toda la lógica está en el caso de uso y en el dominio. Si una ejecución falla, se registra
 * en el log (sin trazas hacia ningún cliente) y la siguiente se intenta de nuevo; el proceso no se detiene.
 * El intervalo es técnico (no un parámetro de la Ficha) y se puede ajustar con la propiedad
 * {@code sga.alojamiento.intervalo-vencimiento-minutos} (por defecto, 5 minutos).
 */
@Component
public class VencimientoProgramado {

    private static final Logger log = LoggerFactory.getLogger(VencimientoProgramado.class);

    private final VencerReservasPendientesUseCase vencerReservas;
    private final Clock reloj;

    public VencimientoProgramado(VencerReservasPendientesUseCase vencerReservas, Clock reloj) {
        this.vencerReservas = vencerReservas;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${sga.alojamiento.intervalo-vencimiento-minutos:5}", timeUnit = TimeUnit.MINUTES)
    public void ejecutar() {
        try {
            int vencidas = vencerReservas.ejecutar(LocalDateTime.now(reloj));
            if (vencidas > 0) {
                log.info("Vencimiento automático: {} reserva(s) pendiente(s) pasaron a CANCELADA", vencidas);
            }
        } catch (RuntimeException ex) {
            log.error("Falló el vencimiento automático de reservas pendientes", ex);
        }
    }
}
