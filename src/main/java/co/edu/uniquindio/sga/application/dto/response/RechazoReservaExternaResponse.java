package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

/** RN-18: constancia de una reserva externa rechazada por conflicto. */
public record RechazoReservaExternaResponse(
        Long id,
        CanalOrigen canalOrigen,
        /** Código del canal que envió el mensaje (la parte del identificador antes de ":"); nulo en reservas sembradas. */
        String canal,
        String identificadorExterno,
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        String motivo,
        LocalDateTime rechazadaEn,
        /** 7.8: el administrador ya lo revisó. */
        boolean revisado,
        String notaRevision,
        String revisadoPor,
        LocalDateTime revisadoEn) {
}
