package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Consulta paginada (10 por página) de los bloqueos de un apartamento. Solo lectura. */
@Service
public class ListarBloqueosUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final BloqueoRepository bloqueoRepository;

    public ListarBloqueosUseCase(ApartamentoRepository apartamentoRepository, BloqueoRepository bloqueoRepository) {
        this.apartamentoRepository = apartamentoRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Bloqueo> ejecutar(IdentificacionApartamento apartamento, SolicitudPagina pagina) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        return bloqueoRepository.listarPorApartamento(apartamento, pagina);
    }
}
