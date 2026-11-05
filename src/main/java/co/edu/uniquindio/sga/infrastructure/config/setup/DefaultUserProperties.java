package co.edu.uniquindio.sga.infrastructure.config.setup;

import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Usuarios iniciales leídos de application.properties, uno por rol (Guía 12, 12.3). Spring lo llena por constructor. */
@ConfigurationProperties(prefix = "sga.usuarios-iniciales")
public record DefaultUserProperties(List<DefaultUser> usuarios) {

    public DefaultUserProperties {
        usuarios = usuarios == null ? List.of() : List.copyOf(usuarios);
    }

    public record DefaultUser(
            String nombre,
            String correo,
            String contrasena,
            String telefono,
            String documento,
            String fechaNacimiento,
            RolUsuario rol) {
    }
}
