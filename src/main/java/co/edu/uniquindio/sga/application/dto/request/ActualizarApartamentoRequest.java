package co.edu.uniquindio.sga.application.dto.request;

import java.util.List;
import java.util.Set;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edita los datos de un apartamento (reemplaza todos). La identificación no se puede cambiar. */
public record ActualizarApartamentoRequest(
        @NotBlank(message = "El nombre del apartamento es obligatorio")
        String nombre,
        @NotBlank(message = "La descripción del apartamento es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,
        @NotNull(message = "El número de dormitorios es obligatorio")
        @Min(value = 1, message = "El apartamento debe tener al menos un dormitorio")
        Integer dormitorios,
        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos de 1 persona")
        Integer capacidad,
        List<@NotBlank(message = "Un elemento de la dotación no puede estar en blanco")
             @Size(max = 60, message = "Un elemento de la dotación no puede superar los 60 caracteres") String> dotacion,
        Set<@NotNull(message = "Una característica no puede ser nula") Caracteristica> caracteristicas) {
}
