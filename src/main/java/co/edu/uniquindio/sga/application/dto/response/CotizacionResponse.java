package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CotizacionResponse(
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        List<CargoNocheResponse> desglose,
        long total,
        String moneda) {
}
