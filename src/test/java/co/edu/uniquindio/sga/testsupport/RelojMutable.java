package co.edu.uniquindio.sga.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj de pruebas que se queda quieto en un instante y que la prueba puede mover: es lo que permite recorrer por HTTP
 * hechos que dependen del paso del tiempo (llegar el día de entrada, declarar un no-show pasada la hora límite,
 * cancelar con distinta antelación, dejar vencer una reserva pendiente) sin esperar y sin depender del día real.
 * Todo lo que recibe un {@code Clock} (controllers, datos sembrados, vencimiento) ve el mismo "ahora".
 */
public class RelojMutable extends Clock {

    private final ZoneId zona;
    private volatile Instant instante;

    public RelojMutable(ZoneId zona, LocalDateTime inicial) {
        this.zona = zona;
        this.instante = inicial.atZone(zona).toInstant();
    }

    /** Pone el reloj en un momento exacto, en la zona del alojamiento. */
    public void fijarEn(LocalDateTime momento) {
        this.instante = momento.atZone(zona).toInstant();
    }

    public void avanzar(Duration duracion) {
        this.instante = instante.plus(duracion);
    }

    public LocalDateTime ahora() {
        return LocalDateTime.ofInstant(instante, zona);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otraZona) {
        return Clock.fixed(instante, otraZona);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
