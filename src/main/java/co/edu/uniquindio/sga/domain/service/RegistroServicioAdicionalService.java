package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/**
 * L-17: el código de un servicio adicional es único. Es una regla que el servicio no puede evaluar solo (necesita
 * conocer los demás servicios), por eso vive en un servicio de dominio. La validez propia del servicio (nombre, valor
 * coherente con "genera cargo") la protege su constructor.
 */
public class RegistroServicioAdicionalService {

    private final ServicioAdicionalRepository servicios;

    public RegistroServicioAdicionalService(ServicioAdicionalRepository servicios) {
        this.servicios = servicios;
    }

    public ServicioAdicional crear(String codigo, String nombre, String descripcion, boolean generaCargo, Dinero valor) {
        ServicioAdicional nuevo = new ServicioAdicional(codigo, nombre, descripcion, generaCargo, valor);
        if (servicios.buscarPorCodigo(nuevo.getCodigo()).isPresent()) {
            throw new ReglaDominioException("Ya existe un servicio adicional con el código " + nuevo.getCodigo());
        }
        return nuevo;
    }
}
