package co.edu.uniquindio.sga.application.dto.request;

import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 7.8: registrar un canal de venta externo. La credencial no se envía: la genera el sistema y se muestra una sola vez. */
public record CrearCanalVentaRequest(
        @NotBlank(message = "El código del canal es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,11}$",
                message = "El código debe tener de 2 a 12 caracteres: letras, números, guion o guion bajo")
        String codigo,

        @NotBlank(message = "El nombre del canal es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotNull(message = "El tipo del canal es obligatorio")
        TipoCanalVenta tipo) {
}
