package co.edu.uniquindio.sga.infrastructure.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** 401 frente a 403 (Guía 12, 3.14) con el ErrorResponse único del proyecto, nunca un 500. */
class GlobalExceptionHandlerSeguridadTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reservas");

    @Test
    void elAccesoDenegadoPorPreAuthorizeEs403() {
        ResponseEntity<ErrorResponse> respuesta =
                handler.handleAccesoDenegado(new AccessDeniedException("Access Denied"), request);

        assertEquals(403, respuesta.getStatusCode().value());
        assertEquals("/api/reservas", respuesta.getBody().path());
    }

    @Test
    void sinTokenEs401ConUnMensajeQueExplicaComoAutenticarse() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleAutenticacion(
                new InsufficientAuthenticationException("Full authentication is required"), request);

        assertEquals(401, respuesta.getStatusCode().value());
        assertEquals(true, respuesta.getBody().mensaje().contains("Bearer"));
    }

    @Test
    void unTokenInvalidoOVencidoEs401() {
        ResponseEntity<ErrorResponse> respuesta =
                handler.handleAutenticacion(new InvalidBearerTokenException("Jwt expired"), request);

        assertEquals(401, respuesta.getStatusCode().value());
    }
}
