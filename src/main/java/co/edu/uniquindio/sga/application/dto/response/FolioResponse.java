package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Detalle del folio. Un saldo negativo es saldo a favor del huésped. Valores en pesos COP sin decimales. */
public record FolioResponse(
        String codigoReserva,
        List<CargoResponse> cargos,
        List<AjusteResponse> ajustes,
        List<PagoResponse> pagos,
        long saldo,
        String moneda,
        boolean cerrado,
        String autorizacionCierre) {

    public record CargoResponse(String concepto, long valor) {
    }

    public record AjusteResponse(String concepto, long valor) {
    }

    public record PagoResponse(String medio, LocalDate fecha, long valor) {
    }
}
