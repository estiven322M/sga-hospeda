package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de entrada del registro público (Guía 12, actividad 7.5 punto 2; proyecto 7.2).
 * No trae rol: el registro público SIEMPRE crea huéspedes; los roles internos los asigna un administrador.
 * Bean Validation revisa la forma (400): campos obligatorios, formato del correo y contraseña segura.
 */
public record RegistroUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,72}$",
                message = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula y un número")
        String contrasena,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?\\d{7,15}$", message = "El teléfono solo admite dígitos (7 a 15) y un + inicial")
        String telefono,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 15, message = "El documento no puede superar 15 caracteres")
        String documento,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento) {
}
