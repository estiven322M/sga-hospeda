package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RegistroSalidaService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * CU-07 Registrar salida. Orquesta, en una sola transacción, los tres agregados que intervienen:
 *  - RegistroSalidaService exige el folio cerrado (7.6; con saldo pendiente el cierre ya requirió autorización, RN-17);
 *  - Reserva.registrarSalida: EN_CURSO → FINALIZADA (RN-08);
 *  - Apartamento.liberar: OCUPADO → PENDIENTE_PREPARACION.
 */
@Service
public class RegistrarSalidaUseCase {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final RegistroSalidaService registroSalidaService;

    public RegistrarSalidaUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                  ApartamentoRepository apartamentoRepository,
                                  RegistroSalidaService registroSalidaService) {
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.registroSalidaService = registroSalidaService;
    }

    @Transactional
    public Reserva ejecutar(CodigoReserva codigo, String autor, LocalDateTime ahora) {
        Reserva reserva = reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(reserva.getApartamento())
                .orElseThrow(() -> new ApartamentoNoEncontradoException(reserva.getApartamento()));

        registroSalidaService.verificarQueElFolioEstaCerrado(reserva, folio);
        reserva.registrarSalida(autor, ahora);
        apartamento.liberar();

        apartamentoRepository.guardar(apartamento);
        return reservaRepository.guardar(reserva);
    }
}
