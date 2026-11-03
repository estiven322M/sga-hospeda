package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/** Consulta del folio de una reserva con su detalle y saldo (7.7). Solo lectura; 404 si no existe. */
@Service
public class ConsultarFolioUseCase {

    private final FolioRepository folioRepository;

    public ConsultarFolioUseCase(FolioRepository folioRepository) {
        this.folioRepository = folioRepository;
    }

    @Transactional(readOnly = true)
    public Folio ejecutar(CodigoReserva reserva) {
        return folioRepository.buscarPorReserva(reserva)
                .orElseThrow(() -> new FolioNoEncontradoException(reserva));
    }
}
