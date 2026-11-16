package co.edu.uniquindio.sga.infrastructure.config;

import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;

/**
 * Siembra el alojamiento (L-01, L-02, L-12, L-16) desde application.properties la primera vez que arranca el sistema. NO
 * depende de los datos de prueba: el alojamiento debe existir también en un sistema desplegado. Es idempotente: si ya
 * existe, no lo toca (lo que el administrador editó se conserva).
 */
@Component
@Order(0)
class InicializadorAlojamiento implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(InicializadorAlojamiento.class);

    private final AlojamientoRepository alojamientos;
    private final String nombre;
    private final String descripcion;
    private final String ciudad;
    private final String direccion;
    private final double latitud;
    private final double longitud;
    private final String horaEntrada;
    private final String horaSalida;
    private final String normas;
    private final String politicaMascotas;

    InicializadorAlojamiento(AlojamientoRepository alojamientos,
                             @Value("${sga.alojamiento.nombre}") String nombre,
                             @Value("${sga.alojamiento.descripcion}") String descripcion,
                             @Value("${sga.alojamiento.ciudad}") String ciudad,
                             @Value("${sga.alojamiento.direccion}") String direccion,
                             @Value("${sga.alojamiento.latitud}") double latitud,
                             @Value("${sga.alojamiento.longitud}") double longitud,
                             @Value("${sga.alojamiento.hora-entrada}") String horaEntrada,
                             @Value("${sga.alojamiento.hora-salida}") String horaSalida,
                             @Value("${sga.alojamiento.normas-convivencia}") String normas,
                             @Value("${sga.alojamiento.politica-mascotas}") String politicaMascotas) {
        this.alojamientos = alojamientos;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.normas = normas;
        this.politicaMascotas = politicaMascotas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (alojamientos.obtener().isPresent()) {
            REGISTRO.info("El alojamiento ya está registrado; no se vuelve a sembrar.");
            return;
        }
        alojamientos.guardar(new Alojamiento(nombre, descripcion, ciudad, direccion, new Ubicacion(latitud, longitud),
                LocalTime.parse(horaEntrada), LocalTime.parse(horaSalida), normas, politicaMascotas));
        REGISTRO.info("Alojamiento sembrado desde application.properties: {}", nombre);
    }
}
