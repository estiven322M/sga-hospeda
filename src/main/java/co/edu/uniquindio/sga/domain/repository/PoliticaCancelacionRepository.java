package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;
import java.util.Optional;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/** Puerto de salida de la política de cancelación versionada (F-07). */
public interface PoliticaCancelacionRepository {

    PoliticaCancelacion guardar(PoliticaCancelacion politica);

    /** La versión que rige en esa fecha (la de mayor vigenteDesde que no supere la fecha). */
    Optional<PoliticaCancelacion> vigenteEn(LocalDate fecha);

    /** La versión congelada en una reserva (RN-13). */
    Optional<PoliticaCancelacion> porVersion(VersionPolitica version);
}
