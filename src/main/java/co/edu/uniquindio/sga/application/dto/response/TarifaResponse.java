package co.edu.uniquindio.sga.application.dto.response;

/** Valor por ocupante facturable y por noche, en pesos COP sin decimales. */
public record TarifaResponse(String temporada, long valorPorOcupanteNoche, String moneda) {
}
