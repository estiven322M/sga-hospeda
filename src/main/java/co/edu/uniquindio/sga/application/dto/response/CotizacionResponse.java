package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Cotización de una estancia. {@code subtotalBruto} es la suma noche por noche (RN-05); {@code descuento} es el descuento
 * por estancia larga (RP-02) calculado una sola vez sobre el bruto; {@code total} = bruto menos descuento, en pesos COP.
 */
public record CotizacionResponse(
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        List<CargoNocheResponse> desglose,
        long subtotalBruto,
        int porcentajeDescuento,
        long descuento,
        long total,
        String moneda) {
}
