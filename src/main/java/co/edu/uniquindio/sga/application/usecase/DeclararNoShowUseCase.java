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
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RetencionCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/**
 * Declarar no-show (CONFIRMADA → NO_SHOW). Orquesta, no decide:
 *  - Reserva.declararNoShow solo lo permite desde la hora límite configurada del día de entrada (RN-08);
 *  - RetencionCancelacionService calcula la consecuencia con la política congelada en la reserva (RN-13);
 *  - Folio.liquidarNoShow revierte el alojamiento y registra la penalidad, dejando la devolución como saldo a favor.
 * La reserva deja de estar activa y libera sus noches (RN-12).
 */
@Service
public class DeclararNoShowUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final PoliticaCancelacionRepository politicaRepository;
    private final RetencionCancelacionService retencionService;
    private final ConfiguracionAlojamiento configuracion;

    public DeclararNoShowUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                 PoliticaCancelacionRepository politicaRepository,
                                 RetencionCancelacionService retencionService,
                                 ConfiguracionAlojamiento configuracion) {
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.politicaRepository = politicaRepository;
        this.retencionService = retencionService;
        this.configuracion = configuracion;
    }

    @Transactional
    public Reserva ejecutar(CodigoReserva codigo, String autor, LocalDateTime ahora) {
        Reserva reserva = reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
        PoliticaCancelacion politica = politicaRepository.porVersion(reserva.getPolitica())
                .orElseThrow(() -> new PoliticaCancelacionNoEncontradaException(
                        "versión " + reserva.getPolitica().numero()));

        reserva.declararNoShow(configuracion.horaLimiteNoShow(), autor, ahora);
        Dinero retencion = retencionService.retencionPorNoShow(reserva, politica);
        folio.liquidarNoShow(retencion);

        folioRepository.guardar(folio);
        return reservaRepository.guardar(reserva);
    }
}
