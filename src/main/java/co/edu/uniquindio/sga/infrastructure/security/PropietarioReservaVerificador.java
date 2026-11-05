package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Verifica que el usuario autenticado sea el TITULAR de la reserva (Guía 12, 7.3; proyecto 7.2). El rol no basta:
 * dos huéspedes tienen el mismo rol y uno no puede ver la reserva, el folio ni cancelar la reserva del otro.
 * <p>
 * Compara el claim {@code documento} del token con el documento del titular. Lee el dominio por su puerto
 * (ReservaRepository), pero el dominio no se entera: Reserva sigue sin saber que existen usuarios (3.6).
 * No decide ninguna regla de negocio: solo responde quién puede ver qué.
 * <p>
 * Se usa en {@code @PreAuthorize} como {@code @propietarioReserva.esTitular(#codigo, authentication)}.
 */
@Component("propietarioReserva")
public class PropietarioReservaVerificador {

    private final ReservaRepository reservaRepository;

    public PropietarioReservaVerificador(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public boolean esTitular(String codigo, Authentication authentication) {
        String documento = DocumentoAutenticado.de(authentication);
        if (documento == null) {
            return false;
        }

        CodigoReserva codigoReserva;
        try {
            codigoReserva = new CodigoReserva(codigo);
        } catch (ReglaDominioException formatoInvalido) {
            return false;
        }

        // Si la reserva no existe, también se niega: un huésped no puede averiguar
        // qué códigos de reserva existen probando con 403 frente a 404
        return reservaRepository.buscarPorCodigo(codigoReserva)
                .map(reserva -> reserva.getTitular().getDocumento().numero().equals(documento))
                .orElse(false);
    }

    /** Lectura del claim documento del JWT, compartida por los verificadores de acceso. */
    static final class DocumentoAutenticado {

        private DocumentoAutenticado() {
        }

        static String de(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                return null;
            }
            String documento = jwt.getClaimAsString(JwtTokenProvider.CLAIM_DOCUMENTO);
            return documento == null || documento.isBlank() ? null : documento.trim();
        }
    }
}
