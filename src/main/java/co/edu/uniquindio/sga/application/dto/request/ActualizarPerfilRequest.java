package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 7.2: el perfil solo permite cambiar el nombre y el teléfono. El correo y el documento no se editan. */
public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?\\d{7,15}$", message = "El teléfono solo admite dígitos (7 a 15) y un + inicial")
        String telefono) {
}
