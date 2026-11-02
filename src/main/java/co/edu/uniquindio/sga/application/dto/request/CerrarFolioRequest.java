package co.edu.uniquindio.sga.application.dto.request;

/**
 * Cierre del folio. Con saldo cero no se necesita nada. Con saldo pendiente el administrador debe dejar
 * registrados quién autoriza y por qué (7.7); si falta alguno de los dos, el dominio rechaza el cierre (409).
 */
public record CerrarFolioRequest(
        String autorizadoPor,
        String motivo) {
}
