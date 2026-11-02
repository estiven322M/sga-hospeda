package co.edu.uniquindio.sga.domain.repository;

import java.util.List;
import java.util.Optional;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

public interface ReservaRepository {
	
	/** Reservas en estado PENDIENTE, CONFIRMADA o EN_CURSO de un apartamento. */
    List<Reserva> buscarActivasPorApartamento(IdentificacionApartamento apartamento);

    /** Crea o actualiza la reserva (con historial, ocupantes y valor congelado). */
    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorCodigo(CodigoReserva codigo);

    /** Listado filtrado, ordenado de la más reciente a la más antigua (creadaEn DESC). */
    Pagina<Reserva> buscar(FiltroReservas filtro, SolicitudPagina pagina);

}
