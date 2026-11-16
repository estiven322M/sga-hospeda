package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 7.2: el administrador crea usuarios INTERNOS con su rol (los roles internos solo los asigna un administrador). Los
 * huéspedes se registran solos por /api/auth/registro. Bean Validation revisa la forma (400); que el correo o el
 * documento ya existan lo responde el servicio con 409.
 */
public record CrearUsuarioInternoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE)
        String contrasena,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?\\d{7,15}$", message = "El teléfono solo admite dígitos (7 a 15) y un + inicial")
        String telefono,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 15, message = "El documento no puede superar 15 caracteres")
        String documento,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "RECEPCIONISTA|ADMINISTRADOR|PERSONAL_SERVICIO",
                message = "El rol de un usuario interno debe ser RECEPCIONISTA, ADMINISTRADOR o PERSONAL_SERVICIO")
        String rol) {
}
