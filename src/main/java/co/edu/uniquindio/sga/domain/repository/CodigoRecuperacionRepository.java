package co.edu.uniquindio.sga.domain.repository;

import java.util.Optional;

import co.edu.uniquindio.sga.domain.entity.CodigoRecuperacion;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;

/**
 * Código de recuperación de contraseña vigente de un correo (7.2). Hay uno solo por correo: un código nuevo
 * reemplaza al anterior.
 */
public interface CodigoRecuperacionRepository {

    /** Crea o actualiza el código del correo (también guarda el contador de intentos y el "ya usado"). */
    void guardar(CodigoRecuperacion codigo);

    Optional<CodigoRecuperacion> buscarPorCorreo(CorreoElectronico correo);
}
