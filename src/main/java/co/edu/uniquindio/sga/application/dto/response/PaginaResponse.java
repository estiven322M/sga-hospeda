package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;

/** Envoltorio común de todos los listados: páginas de 10 elementos, numeradas desde 0. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {
}
