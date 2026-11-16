package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;
import co.edu.uniquindio.sga.application.exception.UsuarioYaRegistradoException;
import co.edu.uniquindio.sga.application.security.SecurityService;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;

/** Contrato HTTP de /api/auth (Guía 12): 200 con token, 400 forma, 401 credenciales, 201 registro y 409. */
@ExtendWith(MockitoExtension.class)
class SecurityControllerTest {

    @Mock private SecurityService securityService;
    @Mock private co.edu.uniquindio.sga.application.security.RecuperacionContrasenaService recuperacion;
    private MockMvc mvc;

    private static final String LOGIN = """
            {"correo":"recepcion@sga.local","contrasena":"Recepcion2026"}""";
    private static final String REGISTRO = """
            {"nombre":"Marta Lopez","correo":"marta@correo.com","contrasena":"Segura2026","telefono":"3005550000",
             "documento":"1094555666","fechaNacimiento":"1993-04-02"}""";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SecurityController(securityService, recuperacion))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void loginCorrectoResponde200ConTokenBearerYRoles() throws Exception {
        when(securityService.login(any())).thenReturn(new TokenResponse("eyJ...", "Bearer",
                Instant.parse("2026-11-05T15:30:00Z"), List.of("RECEPCIONISTA")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJ..."))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("RECEPCIONISTA"));
    }

    @Test
    void loginConContrasenaErradaResponde401ConMensajeGenerico() throws Exception {
        when(securityService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void loginDeUnUsuarioInactivoTambienResponde401SinRevelarElMotivo() throws Exception {
        when(securityService.login(any())).thenThrow(new DisabledException("User is disabled"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void loginConCorreoSinFormatoResponde400ConElCampo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"contrasena\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());
        verify(securityService, never()).login(any());
    }

    @Test
    void registroValidoResponde201ComoHuesped() throws Exception {
        when(securityService.registrar(any())).thenReturn(
                new UsuarioRegistradoResponse("Marta Lopez", "marta@correo.com", "1094555666", "HUESPED"));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("HUESPED"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void registroConContrasenaInseguraResponde400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO.replace("Segura2026", "debil")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena").exists());
        verify(securityService, never()).registrar(any());
    }

    @Test
    void registroSinMayusculaNiNumeroResponde400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO.replace("Segura2026", "solominusculas")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena").exists());
    }

    @Test
    void registroSinCamposObligatoriosResponde400PorCampo() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.correo").exists())
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.documento").exists())
                .andExpect(jsonPath("$.errores.fechaNacimiento").exists());
    }

    @Test
    void solicitarRecuperacionResponde200ConUnMensajeGenerico() throws Exception {
        mvc.perform(post("/api/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@correo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", containsString("Si el correo está registrado")));
        verify(recuperacion).solicitar("nadie@correo.com");
    }

    @Test
    void solicitarRecuperacionConUnCorreoInvalidoResponde400() throws Exception {
        mvc.perform(post("/api/auth/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-un-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());
        verify(recuperacion, never()).solicitar(any());
    }

    @Test
    void confirmarRecuperacionResponde200() throws Exception {
        mvc.perform(post("/api/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"laura@correo.com\",\"codigo\":\"123456\",\"contrasenaNueva\":\"Nueva2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", containsString("cambió")));
    }

    @Test
    void confirmarRecuperacionConUnCodigoRechazadoResponde409SinRevelarElMotivo() throws Exception {
        org.mockito.Mockito.doThrow(new co.edu.uniquindio.sga.domain.exception.ReglaDominioException(
                "El código no es válido o ya venció")).when(recuperacion).confirmar(any());

        mvc.perform(post("/api/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"laura@correo.com\",\"codigo\":\"123456\",\"contrasenaNueva\":\"Nueva2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El código no es válido o ya venció"));
    }

    @Test
    void confirmarRecuperacionConDatosInvalidosResponde400PorCampo() throws Exception {
        mvc.perform(post("/api/auth/recuperacion/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"laura@correo.com\",\"codigo\":\"12\",\"contrasenaNueva\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists())
                .andExpect(jsonPath("$.errores.contrasenaNueva").exists());
        verify(recuperacion, never()).confirmar(any());
    }

    @Test
    void registroConCorreoExistenteResponde409() throws Exception {
        when(securityService.registrar(any()))
                .thenThrow(new UsuarioYaRegistradoException("Ya existe un usuario registrado con ese correo"));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", not(containsString("Exception"))));
    }
}
