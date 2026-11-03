package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

public record ReservaResumenResponse(
        String codigo,
        EstadoReserva estado,
        CanalOrigen canalOrigen,
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        String nombreTitular,
        int totalOcupantes,
        long valorTotal,
        LocalDateTime creadaEn) {
}
