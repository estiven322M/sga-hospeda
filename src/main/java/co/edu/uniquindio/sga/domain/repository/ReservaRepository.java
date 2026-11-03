package co.edu.uniquindio.sga.domain.repository;

import java.util.List;
import java.util.Optional;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

public interface ReservaRepository {
	
	/** Reservas en estado PENDIENTE, CONFIRMADA o EN_CURSO de un apartamento. */
    List<Reserva> buscarActivasPorApartamento(IdentificacionApartamento apartamento);

    /** Crea o actualiza la reserva (con historial, ocupantes y valor congelado). */
    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorCodigo(CodigoReserva codigo);

    /** Listado filtrado, ordenado de la más reciente a la más antigua (creadaEn DESC). */
    Pagina<Reserva> buscar(FiltroReservas filtro, SolicitudPagina pagina);

    /**
     * Reservas en estado PENDIENTE creadas estrictamente antes de {@code limite}. Es el insumo del vencimiento
     * automático (RN-21); el caso de uso calcula el límite con PlazoConfirmacion.limiteDeCreacion.
     */
    List<Reserva> buscarPendientesCreadasAntesDe(java.time.LocalDateTime limite);

    /** RN-19: la reserva ya creada para ese mensaje del canal, si existe. */
    Optional<Reserva> buscarPorCanalEIdentificadorExterno(CanalOrigen canal, IdentificadorExterno identificador);

}
