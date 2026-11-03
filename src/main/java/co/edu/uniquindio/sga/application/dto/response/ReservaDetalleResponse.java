package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

public record ReservaDetalleResponse(
        String codigo,
        EstadoReserva estado,
        CanalOrigen canalOrigen,
        String identificadorExterno,
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        LocalTime horaEstimadaLlegada,
        OcupanteResponse titular,
        List<OcupanteResponse> ocupantes,
        long valorTotal,
        String moneda,
        List<CargoNocheResponse> desglose,
        int versionPolitica,
        LocalDateTime creadaEn,
        List<EventoReservaResponse> historial) {
}
