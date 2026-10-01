package co.edu.uniquindio.sga.domain.repository;

import java.util.List;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

public interface ReservaRepository {
	
	/** Reservas en estado PENDIENTE, CONFIRMADA o EN_CURSO de un apartamento. */
    List<Reserva> buscarActivasPorApartamento(IdentificacionApartamento apartamento);

}
