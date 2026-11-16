package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/**
 * L-17, 7.7: cargar un servicio adicional al folio de una reserva. La regla involucra tres agregados (la reserva, el
 * folio y el servicio) que ninguno puede evaluar solo, por eso vive en un servicio de dominio:
 * <ul>
 *   <li>el folio pertenece a la reserva;</li>
 *   <li>la reserva está CONFIRMADA o EN_CURSO (solo se consume un servicio durante una reserva vigente);</li>
 *   <li>el servicio está activo y genera cargo;</li>
 *   <li>la cantidad es al menos 1;</li>
 *   <li>el folio está abierto (lo exige {@link Folio#registrarCargo}).</li>
 * </ul>
 * El cargo queda como un movimiento del folio y no se edita ni se borra (RN-16).
 */
public class CargoServicioAdicionalService {

    public Cargo registrar(Reserva reserva, Folio folio, ServicioAdicional servicio, int cantidad) {
        if (!folio.getReserva().equals(reserva.getCodigo())) {
            throw new ReglaDominioException("El folio no pertenece a la reserva " + reserva.getCodigo().valor());
        }
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA && reserva.getEstado() != EstadoReserva.EN_CURSO) {
            throw new ReglaDominioException("Solo se registran servicios adicionales en reservas CONFIRMADA o EN_CURSO; la reserva "
                    + reserva.getCodigo().valor() + " está " + reserva.getEstado());
        }
        if (!servicio.isActivo()) {
            throw new ReglaDominioException("El servicio adicional " + servicio.getCodigo() + " está dado de baja");
        }
        if (!servicio.generaCargo()) {
            throw new ReglaDominioException("El servicio adicional " + servicio.getCodigo() + " no genera cargo");
        }
        if (cantidad < 1) {
            throw new ReglaDominioException("La cantidad del servicio adicional debe ser al menos 1");
        }
        Cargo cargo = new Cargo("Servicio adicional: " + servicio.getNombre() + " x" + cantidad,
                servicio.getValor().por(cantidad));
        folio.registrarCargo(cargo); // rechaza si el folio está cerrado
        return cargo;
    }
}
