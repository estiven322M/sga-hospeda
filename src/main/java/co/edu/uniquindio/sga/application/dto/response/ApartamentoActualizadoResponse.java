package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;
import java.util.Set;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;

/**
 * Respuesta de PUT /api/apartamentos/{identificacion}: los mismos campos de {@link ApartamentoResponse} más el aviso de
 * capacidad (7.3). Si se bajó la capacidad, {@code reservasPorEncimaDeLaCapacidad} lista las reservas activas que quedan con
 * más ocupantes que la nueva capacidad; la actualización NO se bloquea y esas reservas no se modifican. Vacía si no hay.
 */
public record ApartamentoActualizadoResponse(
        String identificacion,
        String nombre,
        String descripcion,
        int dormitorios,
        int capacidad,
        List<String> dotacion,
        Set<Caracteristica> caracteristicas,
        EstadoOperativo estadoOperativo,
        boolean activo,
        /** URL de la imagen principal (7.3); nula si el apartamento aún no tiene imágenes. */
        String imagenPrincipal,
        /** La galería: las imágenes sin retirar, en el orden en que se agregaron. */
        List<ImagenResponse> imagenes,
        List<ReservaResumenResponse> reservasPorEncimaDeLaCapacidad) {
}
