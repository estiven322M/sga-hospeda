package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 7.3, L-01, L-02, L-12, L-16. Bean Validation revisa la forma; las reglas del alojamiento las aplica el dominio. */
public record ActualizarAlojamientoRequest(
        @NotBlank(message = "El nombre del alojamiento es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,
        @NotBlank(message = "La descripción del alojamiento es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,
        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 80, message = "La ciudad no puede superar 80 caracteres")
        String ciudad,
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 160, message = "La dirección no puede superar 160 caracteres")
        String direccion,
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90")
        Double latitud,
        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180")
        Double longitud,
        @NotNull(message = "La hora de entrada es obligatoria")
        LocalTime horaEntrada,
        @NotNull(message = "La hora de salida es obligatoria")
        LocalTime horaSalida,
        @NotBlank(message = "Las normas de convivencia son obligatorias")
        @Size(max = 2000, message = "Las normas no pueden superar 2000 caracteres")
        String normasConvivencia,
        @NotBlank(message = "La política de mascotas es obligatoria")
        @Size(max = 2000, message = "La política de mascotas no puede superar 2000 caracteres")
        String politicaMascotas) {
}
