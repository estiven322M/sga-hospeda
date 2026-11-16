package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalTime;
import java.util.List;

/**
 * Los datos públicos del alojamiento (7.3). {@code mediosPago} es la lista de medios aceptados (L-18, configuración del
 * sistema, de solo lectura aquí); el administrador edita el resto con PUT.
 */
public record AlojamientoResponse(
        String nombre,
        String descripcion,
        String ciudad,
        String direccion,
        double latitud,
        double longitud,
        LocalTime horaEntrada,
        LocalTime horaSalida,
        String normasConvivencia,
        String politicaMascotas,
        List<String> mediosPago) {
}
