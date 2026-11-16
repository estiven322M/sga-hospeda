package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FiltroApartamentos;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/**
 * Listado de apartamentos (activos o no), de 10 en 10, con filtro opcional por características (L-06, 7.5).
 * Solo lectura: delega el filtro en el puerto, no decide ninguna regla.
 */
@Service
public class ListarApartamentosUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public ListarApartamentosUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Apartamento> ejecutar(FiltroApartamentos filtro, SolicitudPagina pagina) {
        return apartamentoRepository.listarTodos(filtro, pagina);
    }
}
