package co.edu.uniquindio.sga.domain.service;

import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/**
 * L-18: solo se registran pagos con un medio de pago ACEPTADO por el alojamiento (la lista es configuración, regla 10).
 * El folio no conoce la configuración, por eso la regla vive en un servicio de dominio. La comparación no distingue
 * mayúsculas. Que el folio esté abierto lo sigue exigiendo {@link Folio#registrarPago}.
 */
public class RegistroPagoService {

    private final ConfiguracionAlojamiento configuracion;

    public RegistroPagoService(ConfiguracionAlojamiento configuracion) {
        this.configuracion = configuracion;
    }

    public void registrar(Folio folio, Pago pago) {
        List<String> aceptados = configuracion.mediosPagoAceptados();
        boolean aceptado = aceptados.stream().anyMatch(medio -> medio.equalsIgnoreCase(pago.medio().trim()));
        if (!aceptado) {
            throw new ReglaDominioException("El medio de pago " + pago.medio().trim() + " no es aceptado; los medios aceptados son: "
                    + String.join(", ", aceptados));
        }
        folio.registrarPago(pago);
    }
}
