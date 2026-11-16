package co.edu.uniquindio.sga.application.dto.response;

/** Un servicio adicional (L-17). {@code valor} en pesos COP; {@code activo} indica si se puede cargar a un folio. */
public record ServicioAdicionalResponse(
        String codigo,
        String nombre,
        String descripcion,
        boolean generaCargo,
        long valor,
        String moneda,
        boolean activo) {
}
