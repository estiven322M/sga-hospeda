package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ServicioAdicionalNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/** L-17: editar los datos de un servicio adicional. El código no cambia. Los cargos ya hechos conservan su valor. */
@Service
public class ActualizarServicioAdicionalUseCase {

    private final ServicioAdicionalRepository servicios;

    public ActualizarServicioAdicionalUseCase(ServicioAdicionalRepository servicios) {
        this.servicios = servicios;
    }

    @Transactional
    public ServicioAdicional ejecutar(String codigo, String nombre, String descripcion, boolean generaCargo, Dinero valor) {
        ServicioAdicional servicio = servicios.buscarPorCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ServicioAdicionalNoEncontradoException(codigo));
        servicio.actualizarDatos(nombre, descripcion, generaCargo, valor);
        return servicios.guardar(servicio);
    }
}
