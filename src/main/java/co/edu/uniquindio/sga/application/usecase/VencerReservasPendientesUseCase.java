package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;

/**
 * RN-21 Vencimiento automático: una reserva PENDIENTE que supera el plazo de confirmación pasa a CANCELADA y
 * libera sus noches (RN-12), sin intervención manual. Lo dispara un proceso programado, no un endpoint.
 * Orquesta, no decide:
 *  - el plazo sale de la configuración (L-14) y el instante límite lo calcula PlazoConfirmacion;
 *  - Reserva.haVencido / Reserva.vencer aplican la regla y la transición (RN-08);
 *  - Folio.liquidarVencimiento revierte el alojamiento sin penalidad.
 * Es idempotente: una reserva ya cancelada no vuelve a aparecer entre las pendientes.
 */
@Service
public class VencerReservasPendientesUseCase {

    private static final String AUTOR_SISTEMA = "SISTEMA";

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final ConfiguracionAlojamiento configuracion;

    public VencerReservasPendientesUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                           ConfiguracionAlojamiento configuracion) {
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.configuracion = configuracion;
    }

    /** @return cuántas reservas se vencieron en esta ejecución */
    @Transactional
    public int ejecutar(LocalDateTime ahora) {
        PlazoConfirmacion plazo = configuracion.plazoConfirmacion();
        List<Reserva> candidatas = reservaRepository.buscarPendientesCreadasAntesDe(plazo.limiteDeCreacion(ahora));

        int vencidas = 0;
        for (Reserva reserva : candidatas) {
            if (!reserva.haVencido(plazo, ahora)) {
                continue; // defensa: el puerto es una optimización, la regla la decide el dominio
            }
            Folio folio = folioRepository.buscarPorReserva(reserva.getCodigo())
                    .orElseThrow(() -> new FolioNoEncontradoException(reserva.getCodigo()));
            reserva.vencer(plazo, ahora);
            folio.liquidarVencimiento();
            folioRepository.guardar(folio);
            reservaRepository.guardar(reserva);
            vencidas++;
        }
        return vencidas;
    }
}
