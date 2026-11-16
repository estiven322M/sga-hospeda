package co.edu.uniquindio.sga.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 10.3.1: la reserva que un canal externo crea. Lleva el identificador PROPIO del canal (RN-19): el mismo mensaje recibido dos
 * veces no crea dos reservas. El canal no manda su código ni el canal de origen: salen de su credencial.
 */
public record CrearReservaExternaRequest(
        @NotBlank(message = "El identificador externo es obligatorio")
        @Size(max = 47, message = "El identificador externo no puede superar 47 caracteres")
        String identificadorExterno,
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacionApartamento,
        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,
        LocalTime horaEstimadaLlegada,
        // Opcional: a este correo se avisa al titular (la reserva registrada y su cancelación)
        @Email(message = "El correo del titular no tiene un formato válido")
        @Size(max = 120, message = "El correo del titular no puede superar 120 caracteres")
        String correoTitular,
        @NotNull(message = "La reserva debe tener titular")
        @Valid
        OcupanteRequest titular,
        @NotEmpty(message = "La reserva debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes) {
}
