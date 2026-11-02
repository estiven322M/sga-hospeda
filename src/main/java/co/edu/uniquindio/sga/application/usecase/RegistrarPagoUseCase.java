package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/**
 * CU-08 Registrar pago en folio. Orquesta: busca el folio (404) y delega en Folio.registrarPago, que rechaza
 * pagos sobre un folio cerrado. El saldo (cargos + ajustes − pagos) lo calcula el folio (RN-15). Un pago no
 * se edita ni se borra (RN-16): una corrección sería un movimiento inverso.
 */
@Service
public class RegistrarPagoUseCase {

    private final FolioRepository folioRepository;

    public RegistrarPagoUseCase(FolioRepository folioRepository) {
        this.folioRepository = folioRepository;
    }

    @Transactional
    public Folio ejecutar(CodigoReserva reserva, Pago pago) {
        Folio folio = folioRepository.buscarPorReserva(reserva)
                .orElseThrow(() -> new FolioNoEncontradoException(reserva));
        folio.registrarPago(pago);
        return folioRepository.guardar(folio);
    }
}
