package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 7.2: solo un administrador cambia el rol de un usuario. */
public record CambiarRolRequest(
        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "HUESPED|RECEPCIONISTA|ADMINISTRADOR|PERSONAL_SERVICIO",
                message = "El rol debe ser HUESPED, RECEPCIONISTA, ADMINISTRADOR o PERSONAL_SERVICIO")
        String rol) {
}
