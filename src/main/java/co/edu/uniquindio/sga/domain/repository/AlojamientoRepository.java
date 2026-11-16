package co.edu.uniquindio.sga.domain.repository;

import java.util.Optional;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;

/** Puerto del alojamiento. Hay uno solo: se guarda y se consulta sin identificador. */
public interface AlojamientoRepository {

    /** El alojamiento registrado, si ya se sembró. */
    Optional<Alojamiento> obtener();

    /** Crea o reemplaza los datos del alojamiento y devuelve la versión persistida. */
    Alojamiento guardar(Alojamiento alojamiento);
}
