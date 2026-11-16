package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;
import java.util.Set;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;

/** Un apartamento libre para la estancia consultada y lo que costaría (con lo necesario para su tarjeta). */
public record ApartamentoDisponibleResponse(
        String identificacion,
        String nombre,
        String descripcion,
        int dormitorios,
        int capacidad,
        Set<Caracteristica> caracteristicas,
        long total,
        String moneda,
        List<CargoNocheResponse> desglose) {
}
