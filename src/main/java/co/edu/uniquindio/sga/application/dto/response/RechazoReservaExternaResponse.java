package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

/** RN-18: constancia de una reserva externa rechazada por conflicto. */
public record RechazoReservaExternaResponse(
        CanalOrigen canalOrigen,
        String identificadorExterno,
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        String motivo,
        LocalDateTime rechazadaEn) {
}
