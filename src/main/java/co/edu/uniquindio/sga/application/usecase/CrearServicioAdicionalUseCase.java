package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.service.RegistroServicioAdicionalService;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/** L-17: el administrador crea un servicio adicional. El dominio valida el código único y la coherencia valor/cargo. */
@Service
public class CrearServicioAdicionalUseCase {

    private final ServicioAdicionalRepository servicios;
    private final RegistroServicioAdicionalService registro;

    public CrearServicioAdicionalUseCase(ServicioAdicionalRepository servicios, RegistroServicioAdicionalService registro) {
        this.servicios = servicios;
        this.registro = registro;
    }

    @Transactional
    public ServicioAdicional ejecutar(String codigo, String nombre, String descripcion, boolean generaCargo, Dinero valor) {
        return servicios.guardar(registro.crear(codigo, nombre, descripcion, generaCargo, valor));
    }
}
