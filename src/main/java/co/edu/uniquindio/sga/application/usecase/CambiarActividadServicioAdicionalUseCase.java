package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ServicioAdicionalNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;

/**
 * L-17: dar de baja (baja lógica, regla 11) o volver a dar de alta un servicio adicional. Una baja no borra nada: el
 * servicio deja de poderse cargar, y los cargos ya registrados en folios se conservan.
 */
@Service
public class CambiarActividadServicioAdicionalUseCase {

    private final ServicioAdicionalRepository servicios;

    public CambiarActividadServicioAdicionalUseCase(ServicioAdicionalRepository servicios) {
        this.servicios = servicios;
    }

    @Transactional
    public ServicioAdicional darDeBaja(String codigo) {
        ServicioAdicional servicio = buscar(codigo);
        servicio.darDeBaja();
        return servicios.guardar(servicio);
    }

    @Transactional
    public ServicioAdicional activar(String codigo) {
        ServicioAdicional servicio = buscar(codigo);
        servicio.activar();
        return servicios.guardar(servicio);
    }

    private ServicioAdicional buscar(String codigo) {
        return servicios.buscarPorCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ServicioAdicionalNoEncontradoException(codigo));
    }
}
