package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RetencionCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/**
 * CU-05 Cancelar reserva. Orquesta, no decide:
 *  - Reserva.cancelar valida el motivo y la transición (RN-08: solo desde PENDIENTE o CONFIRMADA, antes del registro);
 *  - RetencionCancelacionService calcula la retención con la política congelada en la reserva (RN-13);
 *  - Folio.liquidarCancelacion registra la penalidad como cargo y el reverso del alojamiento como ajuste,
 *    dejando la devolución como saldo a favor (7.4, RN-16).
 * Al dejar de estar activa la reserva libera sus noches de inmediato (RN-12), porque la disponibilidad
 * solo considera reservas activas.
 */
@Service
public class CancelarReservaUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final PoliticaCancelacionRepository politicaRepository;
    private final RetencionCancelacionService retencionService;

    public CancelarReservaUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                  PoliticaCancelacionRepository politicaRepository,
                                  RetencionCancelacionService retencionService) {
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.politicaRepository = politicaRepository;
        this.retencionService = retencionService;
    }

    @Transactional
    public Reserva ejecutar(CodigoReserva codigo, String motivo, String autor, LocalDateTime ahora) {
        Reserva reserva = reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
        // RN-13: la política congelada en la reserva, no la vigente hoy
        PoliticaCancelacion politica = politicaRepository.porVersion(reserva.getPolitica())
                .orElseThrow(() -> new PoliticaCancelacionNoEncontradaException(
                        "versión " + reserva.getPolitica().numero()));

        reserva.cancelar(motivo, autor, ahora);
        Dinero retencion = retencionService.retencionPorCancelacion(reserva, politica, ahora);
        folio.liquidarCancelacion(retencion);

        Reserva guardada = reservaRepository.guardar(reserva);
        folioRepository.guardar(folio);
        return guardada;
    }
}
