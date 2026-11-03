package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CU-03. La fecha y hora actuales las pone el servidor, nunca el cliente. */
public record CrearReservaRequest(
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacionApartamento,
        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,
        @NotNull(message = "El canal de origen es obligatorio")
        CanalOrigen canalOrigen,
        // RN-19: obligatorio en los canales externos (lo exige el dominio) y debe omitirse en los demás
        @Size(max = 60, message = "El identificador externo no puede superar 60 caracteres")
        String identificadorExterno,
        // Opcional al crear; obligatoria para confirmar (RN-09)
        LocalTime horaEstimadaLlegada,
        @NotNull(message = "La reserva debe tener titular")
        @Valid
        OcupanteRequest titular,
        @NotEmpty(message = "La reserva debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes) {
}
