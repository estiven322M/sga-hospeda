package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 7.2: cambio voluntario de contraseña, con la actual y la nueva (la nueva sigue las reglas del registro). */
public record CambiarContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE)
        String contrasenaNueva) {
}
