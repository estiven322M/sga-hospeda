package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/** Un usuario del sistema (7.2). Nunca incluye la contraseña ni su hash. {@code activo} es la eliminación lógica. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String documento,
        LocalDate fechaNacimiento,
        String rol,
        boolean activo) {
}
