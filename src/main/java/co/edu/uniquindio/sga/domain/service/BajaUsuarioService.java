package co.edu.uniquindio.sga.domain.service;

import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/**
 * 7.2: no puede darse de baja a un usuario que sea titular de reservas PENDIENTE, CONFIRMADA o EN_CURSO. El usuario
 * no es del dominio (3.6); el puente con el negocio es el documento, y quien conoce las reservas es el dominio, por eso
 * esta regla vive aquí y no en el servicio de usuarios. Las reservas históricas no impiden la baja y la baja no las toca.
 */
public class BajaUsuarioService {

    private static final List<EstadoReserva> ESTADOS_ACTIVOS =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private final ReservaRepository reservas;

    public BajaUsuarioService(ReservaRepository reservas) {
        this.reservas = reservas;
    }

    /** @throws ReglaDominioException si el titular tiene reservas activas */
    public void verificarPuedeDesactivarse(DocumentoIdentidad titular) {
        for (EstadoReserva estado : ESTADOS_ACTIVOS) {
            long cuantas = reservas.buscar(
                    new FiltroReservas(null, null, estado, null, null, titular), SolicitudPagina.primera()).totalElementos();
            if (cuantas > 0) {
                throw new ReglaDominioException("No se puede desactivar al usuario: es titular de reservas "
                        + estado + " (" + cuantas + "); cancélelas o espere a que terminen");
            }
        }
    }
}
