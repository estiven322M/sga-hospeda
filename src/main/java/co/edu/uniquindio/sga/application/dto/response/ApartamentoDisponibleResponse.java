package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;

/** Un apartamento libre para la estancia consultada y lo que costaría. */
public record ApartamentoDisponibleResponse(
        String identificacion,
        String nombre,
        int dormitorios,
        int capacidad,
        long total,
        String moneda,
        List<CargoNocheResponse> desglose) {
}
