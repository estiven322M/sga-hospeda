package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 7.2: pide un código de recuperación para el correo. La respuesta es siempre la misma, exista o no el correo. */
public record SolicitarRecuperacionRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo) {
}
