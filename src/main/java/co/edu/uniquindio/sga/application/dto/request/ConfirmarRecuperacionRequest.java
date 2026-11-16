package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 7.2: confirma la recuperación con el código de 6 dígitos recibido y fija la contraseña nueva. */
public record ConfirmarRecuperacionRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos")
        String codigo,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE)
        String contrasenaNueva) {
}
