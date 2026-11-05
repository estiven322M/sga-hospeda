package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;
import co.edu.uniquindio.sga.application.exception.UsuarioYaRegistradoException;
import co.edu.uniquindio.sga.application.security.SecurityService;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/** Implementación del inicio de sesión y del registro público con Spring Security (Guía 12, 6.2 y 7.5). */
@Service
public class SecurityServiceImpl implements SecurityService {

    private static final String TIPO_TOKEN = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioJpaDataRepository usuarios;
    private final PasswordEncoder encoder;
    private final Clock reloj;
    private final long expiracionMinutos;

    public SecurityServiceImpl(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                               UsuarioJpaDataRepository usuarios, PasswordEncoder encoder, Clock reloj,
                               @Value("${sga.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.reloj = reloj;
        this.expiracionMinutos = expiracionMinutos;
    }

    @Override
    public TokenResponse login(LoginRequest request) {

        // 1. Busca el usuario (UserConfig) y compara la contraseña con BCrypt. Si falla, lanza una
        //    AuthenticationException (BadCredentials, Disabled...) que el GlobalExceptionHandler convierte en 401
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizar(request.correo()), request.contrasena()));

        // 2. Credenciales correctas: se leen el rol y el documento del usuario autenticado.
        //    Los roles salen del usuario (CustomUserDetails) y NO de authentication.getAuthorities(): Spring
        //    Security 7 agrega ahí autoridades de factor de autenticación (FACTOR_PASSWORD) que no son roles del SGA
        CustomUserDetails usuario = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // 3. Se calcula la vigencia y se firma el token
        Instant ahora = Instant.now(reloj);
        Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);

        String token = jwtTokenProvider.generateTokenAsString(
                authentication.getName(), usuario.usuario().getDocumento(), roles, ahora, expira);

        return new TokenResponse(token, TIPO_TOKEN, expira, roles);
    }

    @Override
    @Transactional
    public UsuarioRegistradoResponse registrar(RegistroUsuarioRequest request) {
        String correo = normalizar(request.correo());
        String documento = request.documento().trim();

        if (usuarios.existsByCorreo(correo)) {
            throw new UsuarioYaRegistradoException("Ya existe un usuario registrado con ese correo");
        }
        if (usuarios.existsByDocumento(documento)) {
            throw new UsuarioYaRegistradoException("Ya existe un usuario registrado con ese documento");
        }

        // El rol NO viene de la petición: el registro público siempre crea huéspedes (7.2)
        UsuarioEntity nuevo = usuarios.save(new UsuarioEntity(
                request.nombre().trim(), correo, encoder.encode(request.contrasena()),
                request.telefono().trim(), documento, request.fechaNacimiento(), RolUsuario.HUESPED));

        return new UsuarioRegistradoResponse(nuevo.getNombre(), nuevo.getCorreo(), nuevo.getDocumento(),
                nuevo.getRol().name());
    }

    /** El correo se guarda y se busca en minúsculas: Laura@SGA.local y laura@sga.local son la misma cuenta. */
    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
