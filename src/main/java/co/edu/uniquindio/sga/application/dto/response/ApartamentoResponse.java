package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;

/** {@code activo} indica si se puede vender; {@code estadoOperativo} es la condición física presente (3.3). */
public record ApartamentoResponse(
        String identificacion,
        String nombre,
        int dormitorios,
        int capacidad,
        EstadoOperativo estadoOperativo,
        boolean activo) {
}
