package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;
import java.util.Set;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;

/** {@code activo} indica si se puede vender; {@code estadoOperativo} es la condición física presente (3.3). */
public record ApartamentoResponse(
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
        List<ImagenResponse> imagenes) {
}
