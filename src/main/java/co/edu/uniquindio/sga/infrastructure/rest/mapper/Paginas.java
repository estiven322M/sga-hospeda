package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import java.util.function.Function;

import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/** Convierte la página del dominio en el envoltorio de respuesta, mapeando cada elemento. */
public final class Paginas {

    private Paginas() {
    }

    public static <T, R> PaginaResponse<R> aResponse(Pagina<T> pagina, Function<T, R> conversion) {
        return new PaginaResponse<>(
                pagina.contenido().stream().map(conversion).toList(),
                pagina.numero(),
                SolicitudPagina.TAMANIO,
                pagina.totalElementos(),
                pagina.totalPaginas());
    }
}
