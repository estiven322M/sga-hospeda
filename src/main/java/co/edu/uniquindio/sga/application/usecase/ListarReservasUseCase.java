package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/**
 * Listado de reservas con filtros opcionales (fechas de entrada, estado, apartamento, canal y titular),
 * de 10 en 10 y de la más reciente a la más antigua. El orden y el filtrado los resuelve el puerto
 * (contrato de ReservaRepository.buscar); aquí solo se orquesta. Solo lectura.
 */
@Service
public class ListarReservasUseCase {

    private final ReservaRepository reservaRepository;

    public ListarReservasUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> ejecutar(FiltroReservas filtro, SolicitudPagina pagina) {
        return reservaRepository.buscar(filtro, pagina);
    }
}
