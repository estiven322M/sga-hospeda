package co.edu.uniquindio.sga.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Desafío opcional de la Guía 12 (7.5, punto 6). Los 401 y 403 que produce la cadena de filtros (sin token, token
 * inválido o vencido) ocurren ANTES de llegar al controller, así que el GlobalExceptionHandler no los ve.
 * Esta clase los reenvía al mismo GlobalExceptionHandler a través del HandlerExceptionResolver de Spring MVC,
 * para que respondan con el ErrorResponse único del proyecto en lugar de un 401 vacío.
 * <p>
 * Se conserva el encabezado estándar {@code WWW-Authenticate: Bearer ...} del servidor de recursos.
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final AuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
    private final HandlerExceptionResolver resolver;

    public ManejadorErroresSeguridad(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    /** 401: no hay token, el token es inválido (firma, algoritmo o emisor) o ya expiró. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException, jakarta.servlet.ServletException {
        bearer.commence(request, response, ex);                 // 401 + WWW-Authenticate
        resolver.resolveException(request, response, null, ex); // cuerpo: ErrorResponse
    }

    /** 403 decidido en la cadena de filtros (por URL). Los 403 de @PreAuthorize los atiende el controller advice. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
