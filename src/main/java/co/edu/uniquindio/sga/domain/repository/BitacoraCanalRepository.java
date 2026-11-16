package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;

/** Puerto de la bitácora de mensajes intercambiados con los canales (7.8). Solo se agrega y se consulta. */
public interface BitacoraCanalRepository {

    void registrar(EventoCanal evento);

    /**
     * Los eventos, del más reciente al más antiguo, de 10 en 10. {@code canal}, {@code desde} y {@code hasta} son opcionales (nulo =
     * no filtrar); las fechas son inclusivas y se comparan con el día del evento.
     */
    Pagina<EventoCanal> listar(String canal, LocalDate desde, LocalDate hasta, SolicitudPagina pagina);
}
