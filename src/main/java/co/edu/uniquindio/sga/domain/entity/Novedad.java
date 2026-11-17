package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Novedad que el personal reporta sobre un apartamento (7.6): un daño, un faltante, algo que revisar. Tiene fecha, autor,
 * descripción y gravedad, y referencia al apartamento solo por su identificación. Es una constancia inmutable: se registra y
 * se consulta en el historial del apartamento; nunca se edita ni se borra. Registrar una novedad NO cambia el estado operativo
 * (declarar un apartamento fuera de servicio sigue siendo una decisión del administrador).
 */
public record Novedad(
        IdentificacionApartamento apartamento,
        LocalDateTime fecha,
        String autor,
        String descripcion,
        GravedadNovedad gravedad) {

    public static final int MAXIMO_DESCRIPCION = 500;

    public Novedad {
        if (apartamento == null) {
            throw new ReglaDominioException("La novedad debe indicar el apartamento");
        }
        if (fecha == null) {
            throw new ReglaDominioException("La novedad debe tener fecha y hora");
        }
        if (autor == null || autor.isBlank()) {
            throw new ReglaDominioException("La novedad debe indicar su autor");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción de la novedad es obligatoria");
        }
        if (gravedad == null) {
            throw new ReglaDominioException("La novedad debe indicar su gravedad");
        }
        autor = autor.trim();
        descripcion = descripcion.trim();
        if (descripcion.length() > MAXIMO_DESCRIPCION) {
            throw new ReglaDominioException(
                    "La descripción de la novedad no puede superar " + MAXIMO_DESCRIPCION + " caracteres");
        }
    }
}
