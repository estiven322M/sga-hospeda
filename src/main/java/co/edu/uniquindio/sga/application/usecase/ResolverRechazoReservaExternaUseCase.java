package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.RechazoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.repository.RechazoReservaExternaRepository;

/**
 * 7.8: el administrador marca un conflicto del canal externo como revisado, con su nota. Solo escribe la revisión del RECHAZO: este
 * caso de uso ni siquiera recibe un repositorio de reservas, así que no puede tocar la reserva vigente.
 */
@Service
public class ResolverRechazoReservaExternaUseCase {

    private final RechazoReservaExternaRepository rechazos;

    public ResolverRechazoReservaExternaUseCase(RechazoReservaExternaRepository rechazos) {
        this.rechazos = rechazos;
    }

    @Transactional
    public RechazoReservaExterna ejecutar(long id, String nota, String revisadoPor, LocalDateTime ahora) {
        RechazoReservaExterna rechazo = rechazos.buscarPorId(id).orElseThrow(() -> new RechazoNoEncontradoException(id));
        rechazo.resolver(nota, revisadoPor, ahora);
        return rechazos.guardar(rechazo);
    }
}
