package co.edu.uniquindio.sga.domain.repository;

import java.util.Optional;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;

/**
 * Puerto de salida de los servicios adicionales (L-17). No hay método de eliminar: la baja es lógica y se expresa con el
 * estado del propio servicio.
 */
public interface ServicioAdicionalRepository {

    /** Crea o actualiza el servicio y devuelve la versión persistida. */
    ServicioAdicional guardar(ServicioAdicional servicio);

    Optional<ServicioAdicional> buscarPorCodigo(String codigo);

    /** Todos los servicios, activos o no, ordenados por código y de 10 en 10. */
    Pagina<ServicioAdicional> listarTodos(SolicitudPagina pagina);

    /** Solo los servicios activos (los que se pueden cargar a un folio), ordenados por código y de 10 en 10. */
    Pagina<ServicioAdicional> listarActivos(SolicitudPagina pagina);
}
