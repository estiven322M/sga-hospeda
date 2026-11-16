package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/** Listar los servicios adicionales de 10 en 10, todos o solo los activos (los que se pueden cargar a un folio). */
@Service
public class ListarServiciosAdicionalesUseCase {

    private final ServicioAdicionalRepository servicios;

    public ListarServiciosAdicionalesUseCase(ServicioAdicionalRepository servicios) {
        this.servicios = servicios;
    }

    @Transactional(readOnly = true)
    public Pagina<ServicioAdicional> ejecutar(boolean soloActivos, SolicitudPagina pagina) {
        return soloActivos ? servicios.listarActivos(pagina) : servicios.listarTodos(pagina);
    }
}
