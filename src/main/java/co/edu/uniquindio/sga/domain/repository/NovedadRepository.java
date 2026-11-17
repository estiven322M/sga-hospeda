package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Puerto de las novedades de los apartamentos (7.6). Solo se agrega y se consulta: no hay edición ni borrado. */
public interface NovedadRepository {

    Novedad registrar(Novedad novedad);

    /** El historial de un apartamento, de la novedad más reciente a la más antigua, de 10 en 10. */
    Pagina<Novedad> listarPorApartamento(IdentificacionApartamento apartamento, SolicitudPagina pagina);
}
