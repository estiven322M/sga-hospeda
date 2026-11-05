package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Rol y canal al crear una reserva (Guía 12, actividad 7.5 punto 3; proyecto 7.1).
 * <ul>
 *   <li>HUESPED: solo por el canal PORTAL y solo a su nombre (el documento del titular es el del token).</li>
 *   <li>RECEPCIONISTA: por el canal DIRECTO (reserva tomada en recepción, por teléfono o en persona).</li>
 *   <li>ADMINISTRADOR: todo lo del recepcionista y, además, el canal EXTERNO (registra las reservas que llegan de
 *       los canales externos, con su identificador, mientras no exista una integración automática).</li>
 *   <li>PERSONAL_SERVICIO: no crea reservas.</li>
 * </ul>
 * <b>¿Por qué aquí y no en el dominio?</b> Es una regla de ACCESO, no de negocio: depende de quién está
 * autenticado y con qué rol, y el dominio no conoce al usuario (3.6). Para el negocio una reserva por PORTAL
 * a nombre de cualquier titular es válida (la puede registrar recepción en nombre de alguien). Las reglas de
 * negocio del canal (RN-18, RN-19) siguen en el dominio. Por eso se evalúa en {@code @PreAuthorize} y su
 * incumplimiento es 403, no 409.
 */
@Component("canalReserva")
public class CanalReservaVerificador {

    private static final Map<RolUsuario, Set<CanalOrigen>> CANALES_POR_ROL = Map.of(
            RolUsuario.HUESPED, EnumSet.of(CanalOrigen.PORTAL),
            RolUsuario.RECEPCIONISTA, EnumSet.of(CanalOrigen.DIRECTO),
            RolUsuario.ADMINISTRADOR, EnumSet.of(CanalOrigen.DIRECTO, CanalOrigen.EXTERNO));

    public boolean puedeCrear(CrearReservaRequest request, Authentication authentication) {
        if (request == null || request.canalOrigen() == null || authentication == null) {
            return false;
        }
        for (GrantedAuthority autoridad : authentication.getAuthorities()) {
            RolUsuario rol = rolDe(autoridad.getAuthority());
            if (rol != null && CANALES_POR_ROL.getOrDefault(rol, Set.of()).contains(request.canalOrigen())
                    && (rol != RolUsuario.HUESPED || esASuNombre(request, authentication))) {
                return true;
            }
        }
        return false;
    }

    /** El huésped solo reserva a su nombre: el titular de la reserva es él mismo. */
    private static boolean esASuNombre(CrearReservaRequest request, Authentication authentication) {
        String documento = PropietarioReservaVerificador.DocumentoAutenticado.de(authentication);
        return documento != null && request.titular() != null && request.titular().documento() != null
                && documento.equals(request.titular().documento().trim());
    }

    private static RolUsuario rolDe(String autoridad) {
        for (RolUsuario rol : RolUsuario.values()) {
            if (rol.name().equals(autoridad)) {
                return rol;
            }
        }
        return null;
    }
}
