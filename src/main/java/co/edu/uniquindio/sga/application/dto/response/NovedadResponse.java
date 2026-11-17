package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;

/** Una novedad del historial de un apartamento (7.6). */
public record NovedadResponse(
        String identificacionApartamento,
        LocalDateTime fecha,
        String autor,
        String descripcion,
        GravedadNovedad gravedad) {
}
