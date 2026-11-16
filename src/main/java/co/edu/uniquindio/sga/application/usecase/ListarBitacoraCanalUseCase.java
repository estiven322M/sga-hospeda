package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.BitacoraCanalRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/** La bitácora de mensajes intercambiados con los canales (7.8): de 10 en 10, del más reciente al más antiguo, con filtros opcionales. */
@Service
public class ListarBitacoraCanalUseCase {

    private final BitacoraCanalRepository bitacora;

    public ListarBitacoraCanalUseCase(BitacoraCanalRepository bitacora) {
        this.bitacora = bitacora;
    }

    @Transactional(readOnly = true)
    public Pagina<EventoCanal> ejecutar(String canal, LocalDate desde, LocalDate hasta, SolicitudPagina pagina) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaDominioException("La fecha final no puede ser anterior a la inicial");
        }
        return bitacora.listar(canal, desde, hasta, pagina);
    }
}
