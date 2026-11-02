package co.edu.uniquindio.sga.domain.repository;

import java.util.List;
import java.util.Optional;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Puerto de salida del agregado Apartamento.
 * El dominio declara qué necesita; la infraestructura (JPA) decide cómo se guarda.
 * No hay método de eliminar: la baja es lógica y se expresa con el estado del propio apartamento.
 */
public interface ApartamentoRepository {

    /** Crea o actualiza el apartamento y devuelve la versión persistida. */
    Apartamento guardar(Apartamento apartamento);

    Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion);

    /** Apartamentos que siguen activos (no dados de baja lógicamente). */
    List<Apartamento> listarActivos();

    /** Listado paginado de los apartamentos activos. */
    Pagina<Apartamento> listarActivos(SolicitudPagina pagina);

}
