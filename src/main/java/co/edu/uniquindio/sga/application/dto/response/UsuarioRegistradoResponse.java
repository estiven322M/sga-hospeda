package co.edu.uniquindio.sga.application.dto.response;

/** DTO de salida del registro público (Guía 12). Nunca devuelve la contraseña ni su hash. */
public record UsuarioRegistradoResponse(
        String nombre,
        String correo,
        String documento,
        String rol) {
}
