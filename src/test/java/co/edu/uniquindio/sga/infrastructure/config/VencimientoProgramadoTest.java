package co.edu.uniquindio.sga.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import co.edu.uniquindio.sga.application.usecase.VencerReservasPendientesUseCase;

/** El "reloj" del vencimiento automático (RN-21): solo dispara el caso de uso y nunca se detiene por un error. */
class VencimientoProgramadoTest {

    // 10:00 hora de Colombia
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final VencerReservasPendientesUseCase caso = mock(VencerReservasPendientesUseCase.class);
    private final VencimientoProgramado programado = new VencimientoProgramado(caso, reloj);

    @Test
    void ejecutaElCasoDeUsoConLaHoraDelServidor() {
        when(caso.ejecutar(LocalDateTime.of(2026, 11, 2, 10, 0))).thenReturn(2);

        programado.ejecutar();

        verify(caso).ejecutar(LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void unErrorNoDetieneElProcesoProgramado() {
        when(caso.ejecutar(any())).thenThrow(new IllegalStateException("la base de datos no responde"));

        assertDoesNotThrow(programado::ejecutar); // se registra en el log; la próxima ejecución lo reintenta
        verify(caso).ejecutar(any());
    }

    @Test
    void estaProgramadoYElIntervaloEsConfigurableEnMinutos() throws Exception {
        Method metodo = VencimientoProgramado.class.getMethod("ejecutar");
        Scheduled scheduled = metodo.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals(TimeUnit.MINUTES, scheduled.timeUnit());
        assertTrue(scheduled.fixedDelayString().contains("intervalo-vencimiento-minutos"));
        assertNotNull(ProgramacionConfig.class.getAnnotation(EnableScheduling.class));
    }
}
