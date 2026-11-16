package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 7.8: la nota con la que el administrador marca un conflicto del canal externo como revisado. */
public record ResolverRechazoRequest(
        @NotBlank(message = "La nota es obligatoria: qué se revisó y qué se decidió")
        @Size(max = 500, message = "La nota no puede superar 500 caracteres")
        String nota) {
}
