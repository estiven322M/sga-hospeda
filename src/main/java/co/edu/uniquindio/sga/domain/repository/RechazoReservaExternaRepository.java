package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;

/** Puerto de los rechazos de reservas externas (RN-18). Solo se agregan; nunca se modifican ni se borran. */
public interface RechazoReservaExternaRepository {

    /** Inserta un rechazo nuevo o guarda su revisión (7.8): el resto de sus datos nunca cambia. */
    RechazoReservaExterna guardar(RechazoReservaExterna rechazo);

    java.util.Optional<RechazoReservaExterna> buscarPorId(long id);

    /** Listado filtrado: {@code revisado} nulo = todos; true = solo los ya revisados; false = solo los pendientes. */
    Pagina<RechazoReservaExterna> listar(Boolean revisado, SolicitudPagina pagina);

    /** Listado paginado, del rechazo más reciente al más antiguo. */
    default Pagina<RechazoReservaExterna> listar(SolicitudPagina pagina) {
        return listar(null, pagina);
    }
}
