package co.edu.uniquindio.sga.application.security;

import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;

/**
 * Puerto de autenticación (Guía 12, 6.2). El controller depende de esta interfaz, no de Spring Security;
 * la implementación vive en infraestructura porque usa el AuthenticationManager.
 */
public interface SecurityService {

    /** Verifica las credenciales y emite el JWT. Credenciales inválidas o usuario inactivo: 401. */
    TokenResponse login(LoginRequest request);

    /** Registro público: crea SIEMPRE un huésped. Correo o documento ya registrados: 409. */
    UsuarioRegistradoResponse registrar(RegistroUsuarioRequest request);
}
