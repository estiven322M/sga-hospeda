package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/**
 * Las reservas de UN titular, de 10 en 10 y de la más reciente a la más antigua, con filtro opcional de estado.
 * Es la consulta "mis reservas" del huésped. El documento llega ya resuelto por quien llama (la capa REST lo toma del
 * token), no de un parámetro de la petición: así un huésped no puede pedir las reservas de otro. Solo orquesta: el
 * orden y el filtrado los resuelve el puerto (ReservaRepository.buscar). Solo lectura.
 */
@Service
public class ListarMisReservasUseCase {

    private final ReservaRepository reservaRepository;

    public ListarMisReservasUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> ejecutar(DocumentoIdentidad titular, EstadoReserva estado, SolicitudPagina pagina) {
        return reservaRepository.buscar(
                new FiltroReservas(null, null, estado, null, null, titular), pagina);
    }
}
