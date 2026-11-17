package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Historial de novedades de un apartamento (7.6): de 10 en 10, de la más reciente a la más antigua. Solo lectura. */
@Service
public class ListarNovedadesUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final NovedadRepository novedadRepository;

    public ListarNovedadesUseCase(ApartamentoRepository apartamentoRepository, NovedadRepository novedadRepository) {
        this.apartamentoRepository = apartamentoRepository;
        this.novedadRepository = novedadRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Novedad> ejecutar(IdentificacionApartamento apartamento, SolicitudPagina pagina) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        return novedadRepository.listarPorApartamento(apartamento, pagina);
    }
}
