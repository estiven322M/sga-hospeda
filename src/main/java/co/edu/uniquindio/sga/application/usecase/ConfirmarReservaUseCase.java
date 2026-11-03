package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * Confirmar reserva (PENDIENTE → CONFIRMADA). Orquesta, no decide:
 *  - la hora estimada de llegada, si el cliente la informa, se registra en la reserva (RN-09 la exige para confirmar);
 *  - ConfirmacionReservaService verifica que el folio cubra el anticipo configurado y que la transición sea válida
 *    (RN-08) y confirma. Si algo falla, la transacción se revierte y no se guarda nada.
 */
@Service
public class ConfirmarReservaUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final ConfirmacionReservaService confirmacionService;

    public ConfirmarReservaUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                   ConfirmacionReservaService confirmacionService) {
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.confirmacionService = confirmacionService;
    }

    /** @param horaEstimadaLlegada opcional: se registra antes de confirmar; si es null se usa la que ya tenga la reserva */
    @Transactional
    public Reserva ejecutar(CodigoReserva codigo, LocalTime horaEstimadaLlegada, String autor, LocalDateTime ahora) {
        Reserva reserva = reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));

        if (horaEstimadaLlegada != null) {
            reserva.indicarHoraEstimadaLlegada(horaEstimadaLlegada, autor, ahora);
        }
        confirmacionService.confirmar(reserva, folio, autor, ahora);
        return reservaRepository.guardar(reserva);
    }
}
