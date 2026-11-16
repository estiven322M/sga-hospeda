package co.edu.uniquindio.sga.domain.repository;

import java.util.Optional;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;

/**
 * Correo opcional que se da al crear una reserva para avisar al titular cuando no tiene cuenta (10.3.2-A). Es un dato
 * de contacto, no del negocio: la reserva y su titular no lo conocen.
 */
public interface ContactoReservaRepository {

    /** Crea o reemplaza el correo de contacto de la reserva. */
    void guardar(CodigoReserva reserva, CorreoElectronico correo);

    Optional<CorreoElectronico> buscar(CodigoReserva reserva);
}
