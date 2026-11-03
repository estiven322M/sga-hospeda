package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/** Traduce entre la paginación del dominio (tamaño fijo, página desde 0) y la de Spring Data. */
public final class PaginacionJpa {

    private PaginacionJpa() {
    }

    public static Pageable solicitud(SolicitudPagina pagina, Sort orden) {
        return PageRequest.of(pagina.numero(), SolicitudPagina.TAMANIO, orden);
    }

    public static <E, D> Pagina<D> aDominio(Page<E> pagina, SolicitudPagina solicitud, Function<E, D> mapeo) {
        return new Pagina<>(pagina.getContent().stream().map(mapeo).toList(), solicitud.numero(),
                pagina.getTotalElements());
    }
}
