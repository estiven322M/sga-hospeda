package co.edu.uniquindio.sga.domain.repository;

import java.util.Optional;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * Puerto de salida del agregado Folio.
 * Un folio pertenece a una única reserva y se referencia solo por su código (regla 3).
 */
public interface FolioRepository {

    /** Crea o actualiza el folio con sus cargos y pagos. */
    Folio guardar(Folio folio);

    Optional<Folio> buscarPorReserva(CodigoReserva reserva);

}
