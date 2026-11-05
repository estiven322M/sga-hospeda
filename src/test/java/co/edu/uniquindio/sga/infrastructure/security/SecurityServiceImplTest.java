package co.edu.uniquindio.sga.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;
import co.edu.uniquindio.sga.application.exception.UsuarioYaRegistradoException;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;

/** Inicio de sesión y registro público (Guía 12, 6.2 y 7.5). */
@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    private static final String SECRETO = "clave-de-pruebas-con-mas-de-32-caracteres-0123";
    private static final Instant AHORA = Instant.parse("2026-11-05T15:00:00Z");

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UsuarioJpaDataRepository usuarios;
    @Mock private PasswordEncoder encoder;

    private final JwtConfig jwtConfig = new JwtConfig(SECRETO, "sga");
    private SecurityServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new SecurityServiceImpl(authenticationManager,
                new JwtTokenProvider(jwtConfig.jwtEncoder(), "sga"), usuarios, encoder,
                Clock.fixed(AHORA, ZoneId.of("America/Bogota")), 30);
    }

    private static UsuarioEntity recepcionista() {
        return new UsuarioEntity("Recepcion", "recepcion@sga.local", "{bcrypt}hash", "3001000002",
                "1000000002", LocalDate.of(1990, 7, 22), RolUsuario.RECEPCIONISTA);
    }

    @Test
    void conCredencialesCorrectasEmiteUnTokenBearerConRolYDocumento() {
        CustomUserDetails principal = new CustomUserDetails(recepcionista());
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        TokenResponse respuesta = servicio.login(new LoginRequest("Recepcion@SGA.local ", "Recepcion2026"));

        assertEquals("Bearer", respuesta.tipo());
        assertEquals(List.of("RECEPCIONISTA"), respuesta.roles());
        assertEquals(AHORA.plusSeconds(30 * 60), respuesta.expiraEn());

        // El token se lee: correo, roles y documento a la vista (firmado, no cifrado). Se valida con un
        // decoder sin validador de tiempo porque el reloj del servicio está fijo en el pasado.
        NimbusJwtDecoder lector = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        lector.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        Jwt jwt = lector.decode(respuesta.token());
        assertEquals("recepcion@sga.local", jwt.getSubject());
        assertEquals("1000000002", jwt.getClaimAsString("documento"));
        assertEquals(List.of("RECEPCIONISTA"), jwt.getClaimAsStringList("roles"));

        // El correo se normaliza antes de autenticar
        ArgumentCaptor<UsernamePasswordAuthenticationToken> intento =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(intento.capture());
        assertEquals("recepcion@sga.local", intento.getValue().getName());
    }

    @Test
    void conCredencialesInvalidasLaExcepcionDeAutenticacionSubeAlManejadorDe401() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> servicio.login(new LoginRequest("recepcion@sga.local", "Equivocada1")));
    }

    private static RegistroUsuarioRequest registro(String correo, String documento) {
        return new RegistroUsuarioRequest("Marta Lopez", correo, "Segura2026", "3005550000", documento,
                LocalDate.of(1993, 4, 2));
    }

    @Test
    void elRegistroPublicoCreaSiempreUnHuespedConLaContrasenaCifrada() {
        when(encoder.encode("Segura2026")).thenReturn("{bcrypt}cifrada");
        when(usuarios.save(any(UsuarioEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioRegistradoResponse respuesta = servicio.registrar(registro(" Marta@Correo.com", "1094555666"));

        ArgumentCaptor<UsuarioEntity> guardado = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarios).save(guardado.capture());
        assertEquals(RolUsuario.HUESPED, guardado.getValue().getRol());
        assertEquals("{bcrypt}cifrada", guardado.getValue().getContrasena());
        assertEquals("marta@correo.com", guardado.getValue().getCorreo());
        assertTrue(guardado.getValue().isActivo());
        assertEquals("HUESPED", respuesta.rol());
        assertEquals("marta@correo.com", respuesta.correo());
    }

    @Test
    void elRegistroConUnCorreoExistenteResponde409() {
        when(usuarios.existsByCorreo("huesped1@sga.local")).thenReturn(true);

        assertThrows(UsuarioYaRegistradoException.class,
                () -> servicio.registrar(registro("huesped1@sga.local", "1094555666")));
        verify(usuarios, never()).save(any());
    }

    @Test
    void elRegistroConUnDocumentoExistenteResponde409() {
        when(usuarios.existsByDocumento("1094000111")).thenReturn(true);

        assertThrows(UsuarioYaRegistradoException.class,
                () -> servicio.registrar(registro("nuevo@correo.com", "1094000111")));
        verify(usuarios, never()).save(any());
    }
}
