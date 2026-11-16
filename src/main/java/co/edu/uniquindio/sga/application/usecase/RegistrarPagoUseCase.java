package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.service.RegistroPagoService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/**
 * CU-08 Registrar pago en folio. Orquesta: busca el folio (404) y delega en el servicio de dominio, que rechaza (409) un
 * medio de pago no aceptado por el alojamiento (L-18) y, a través del folio, un pago sobre un folio cerrado. El saldo
 * (cargos + ajustes − pagos) lo calcula el folio (RN-15). Un pago no se edita ni se borra (RN-16): una corrección sería
 * un movimiento inverso.
 */
@Service
public class RegistrarPagoUseCase {

    private final FolioRepository folioRepository;
    private final RegistroPagoService registroPago;

    public RegistrarPagoUseCase(FolioRepository folioRepository, RegistroPagoService registroPago) {
        this.folioRepository = folioRepository;
        this.registroPago = registroPago;
    }

    @Transactional
    public Folio ejecutar(CodigoReserva reserva, Pago pago) {
        Folio folio = folioRepository.buscarPorReserva(reserva)
                .orElseThrow(() -> new FolioNoEncontradoException(reserva));
        registroPago.registrar(folio, pago);
        return folioRepository.guardar(folio);
    }
}
