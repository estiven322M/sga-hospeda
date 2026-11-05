package co.edu.uniquindio.sga.infrastructure.rest.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Forma única de todos los errores de la API (400, 401, 403, 404, 409 y 500).
 * {@code errores} solo se llena en un 400 de Bean Validation (campo → mensaje); en los demás va nulo.
 * Nunca contiene trazas ni mensajes técnicos de la infraestructura.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores) {
}
