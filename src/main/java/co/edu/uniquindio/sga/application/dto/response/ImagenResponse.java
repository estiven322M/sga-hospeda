package co.edu.uniquindio.sga.application.dto.response;

/** Una imagen de la galería de un apartamento: su identificador (para elegirla como principal o retirarla), su URL y si es la principal. */
public record ImagenResponse(String id, String url, boolean principal) {
}
