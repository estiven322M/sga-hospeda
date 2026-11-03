package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * Cerrar el folio de una reserva; con saldo pendiente es CU-11 Autorizar cierre de folio con saldo pendiente.
 * Orquesta: Folio.cerrar exige la autorización explícita registrada cuando el saldo no es cero (RN-17) y rechaza
 * cerrar un folio ya cerrado. Una vez cerrado, el folio no admite más movimientos.
 */
@Service
public class CerrarFolioUseCase {

    private final FolioRepository folioRepository;

    public CerrarFolioUseCase(FolioRepository folioRepository) {
        this.folioRepository = folioRepository;
    }

    /** @param autorizacion texto de la autorización del administrador (quién y por qué); null si el saldo es cero */
    @Transactional
    public Folio ejecutar(CodigoReserva reserva, String autorizacion) {
        Folio folio = folioRepository.buscarPorReserva(reserva)
                .orElseThrow(() -> new FolioNoEncontradoException(reserva));
        folio.cerrar(autorizacion);
        return folioRepository.guardar(folio);
    }
}
