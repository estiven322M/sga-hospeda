package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Qué página se pide. El tamaño es fijo (10 por página, rúbrica) y el número parte de 0.
 * Es un objeto puro del dominio: no depende del Pageable de Spring.
 */
public record SolicitudPagina(int numero) {

    public static final int TAMANIO = 10;

    public SolicitudPagina {
        if (numero < 0) {
            throw new ReglaDominioException("El número de página no puede ser negativo");
        }
    }

    public static SolicitudPagina primera() {
        return new SolicitudPagina(0);
    }

    /** Posición del primer elemento de la página dentro del listado completo. */
    public int desplazamiento() {
        return numero * TAMANIO;
    }
}
