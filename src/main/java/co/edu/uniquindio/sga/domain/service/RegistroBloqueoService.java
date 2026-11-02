package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * 7.3: "No puede registrarse un bloqueo sobre noches que ya tengan reservas activas."
 * Es una regla que el Bloqueo no puede evaluar solo (necesita conocer las reservas, otro agregado),
 * por eso vive en un servicio de dominio. La validez propia del bloqueo (rango y motivo) la protege su constructor.
 */
public class RegistroBloqueoService {

    private final ReservaRepository reservaRepository;

    public RegistroBloqueoService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public Bloqueo crear(IdentificacionApartamento apartamento, Estancia rango, String motivo) {
        Bloqueo bloqueo = new Bloqueo(apartamento, rango, motivo);
        for (Reserva reserva : reservaRepository.buscarActivasPorApartamento(apartamento)) {
            if (bloqueo.afecta(reserva.getEstancia())) {
                throw new ReglaDominioException("No se puede bloquear: el apartamento tiene la reserva activa "
                        + reserva.getCodigo().valor() + " en esas noches");
            }
        }
        return bloqueo;
    }
}
