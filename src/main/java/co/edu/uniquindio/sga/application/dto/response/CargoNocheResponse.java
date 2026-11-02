package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/** Una línea del desglose noche por noche (RN-05). Los valores son pesos COP sin decimales. */
public record CargoNocheResponse(
        LocalDate noche,
        String temporada,
        long tarifaPorOcupante,
        int ocupantesFacturables,
        long subtotal) {
}
