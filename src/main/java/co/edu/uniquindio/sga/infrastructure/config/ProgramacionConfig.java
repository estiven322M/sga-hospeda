package co.edu.uniquindio.sga.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (@Scheduled), por ahora el vencimiento automático de reservas pendientes. */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
