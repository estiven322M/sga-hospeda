package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/** Listado de todos los apartamentos (activos o no), de 10 en 10. Solo lectura. */
@Service
public class ListarApartamentosUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public ListarApartamentosUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Apartamento> ejecutar(SolicitudPagina pagina) {
        return apartamentoRepository.listarTodos(pagina);
    }
}
