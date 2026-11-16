package co.edu.uniquindio.sga.application.security;

import java.util.Optional;

import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;

/**
 * Puente entre el titular de una reserva y su cuenta de usuario (3.6): dado el documento, el correo de la cuenta activa
 * que lo tiene. El usuario es de infraestructura, por eso el caso de uso de notificaciones lo consulta por este puerto.
 */
public interface DirectorioCorreos {

    Optional<CorreoElectronico> correoDeCuenta(DocumentoIdentidad documento);
}
