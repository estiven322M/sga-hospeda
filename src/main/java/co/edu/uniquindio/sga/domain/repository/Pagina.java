package co.edu.uniquindio.sga.domain.repository;

import java.util.List;

/**
 * Resultado paginado, puro del dominio. Lo devuelven los puertos de listado
 * y lo usan los casos de uso que paginan resultados calculados en memoria (CU-02).
 */
public record Pagina<T>(List<T> contenido, int numero, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return (int) Math.ceil((double) totalElementos / SolicitudPagina.TAMANIO);
    }

    /** Recorta una lista completa a la página pedida. */
    public static <T> Pagina<T> de(List<T> completa, SolicitudPagina solicitud) {
        int desde = Math.min(solicitud.desplazamiento(), completa.size());
        int hasta = Math.min(desde + SolicitudPagina.TAMANIO, completa.size());
        return new Pagina<>(completa.subList(desde, hasta), solicitud.numero(), completa.size());
    }
}
