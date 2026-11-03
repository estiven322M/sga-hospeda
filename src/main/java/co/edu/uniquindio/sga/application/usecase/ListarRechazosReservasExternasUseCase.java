package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.RechazoReservaExternaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/** Consulta paginada (10 por página) de las reservas externas rechazadas (RN-18), de la más reciente a la más antigua. */
@Service
public class ListarRechazosReservasExternasUseCase {

    private final RechazoReservaExternaRepository rechazoRepository;

    public ListarRechazosReservasExternasUseCase(RechazoReservaExternaRepository rechazoRepository) {
        this.rechazoRepository = rechazoRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<RechazoReservaExterna> ejecutar(SolicitudPagina pagina) {
        return rechazoRepository.listar(pagina);
    }
}
