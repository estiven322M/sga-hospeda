package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;

/** Puerto de los rechazos de reservas externas (RN-18). Solo se agregan; nunca se modifican ni se borran. */
public interface RechazoReservaExternaRepository {

    RechazoReservaExterna guardar(RechazoReservaExterna rechazo);

    /** Listado paginado, del rechazo más reciente al más antiguo. */
    Pagina<RechazoReservaExterna> listar(SolicitudPagina pagina);
}
