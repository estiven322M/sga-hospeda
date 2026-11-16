package co.edu.uniquindio.sga.infrastructure.rest.controller;

import co.edu.uniquindio.sga.application.dto.request.ConfirmarRecuperacionRequest;
import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.request.SolicitarRecuperacionRequest;
import co.edu.uniquindio.sga.application.dto.response.MensajeResponse;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;
import co.edu.uniquindio.sga.application.security.RecuperacionContrasenaService;
import co.edu.uniquindio.sga.application.security.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de autenticación (Guía 12). Ambos endpoints son públicos (SecurityConfig.RUTAS_PUBLICAS):
 * el login emite el JWT y el registro crea huéspedes. Depende del puerto SecurityService, no de Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, emisión del JWT y registro público de huéspedes")
public class SecurityController {

    private final SecurityService securityService;
    private final RecuperacionContrasenaService recuperacion;

    public SecurityController(SecurityService securityService, RecuperacionContrasenaService recuperacion) {
        this.securityService = securityService;
        this.recuperacion = recuperacion;
    }

    /** POST -> /api/auth/login */
    @PostMapping("/login")
    @SecurityRequirements    // público: no exige token en Swagger
    @Operation(summary = "Iniciar sesión", description = "Devuelve un JWT para usar como Bearer token")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(securityService.login(request));
    }

    /**
     * POST -> /api/auth/registro. Crea siempre un HUESPED (7.2). No devuelve Location: las cuentas no se
     * exponen como recurso consultable; el huésped continúa con POST /api/auth/login.
     */
    @PostMapping("/registro")
    @SecurityRequirements    // público: no exige token en Swagger
    @Operation(summary = "Registrarse como huésped",
            description = "Registro público. El rol asignado es siempre HUESPED")
    @ApiResponse(responseCode = "201", description = "Huésped registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña insegura")
    @ApiResponse(responseCode = "409", description = "El correo o el documento ya están registrados")
    public ResponseEntity<UsuarioRegistradoResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.registrar(request));
    }

    /**
     * POST -> /api/auth/recuperacion/solicitar. Público. Responde SIEMPRE lo mismo, exista o no el correo (7.2): así no
     * se puede averiguar qué correos están registrados. Si el correo es de un usuario activo, recibe un código de 6 dígitos.
     */
    @PostMapping("/recuperacion/solicitar")
    @SecurityRequirements    // público: no exige token en Swagger
    @Operation(summary = "Solicitar un código de recuperación de contraseña",
            description = "Respuesta genérica: no revela si el correo está registrado")
    @ApiResponse(responseCode = "200", description = "Solicitud recibida")
    @ApiResponse(responseCode = "400", description = "Correo con formato inválido")
    public ResponseEntity<MensajeResponse> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        recuperacion.solicitar(request.correo());
        return ResponseEntity.ok(new MensajeResponse(
                "Si el correo está registrado, enviamos un código de recuperación que vence en 15 minutos"));
    }

    /** POST -> /api/auth/recuperacion/confirmar. Público. Cualquier problema con el código o el correo: el mismo 409. */
    @PostMapping("/recuperacion/confirmar")
    @SecurityRequirements    // público: no exige token en Swagger
    @Operation(summary = "Cambiar la contraseña con el código recibido",
            description = "El código es de 6 dígitos, vale 15 minutos y sirve una sola vez")
    @ApiResponse(responseCode = "200", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña insegura")
    @ApiResponse(responseCode = "409", description = "El código no es válido o ya venció")
    public ResponseEntity<MensajeResponse> confirmarRecuperacion(@Valid @RequestBody ConfirmarRecuperacionRequest request) {
        recuperacion.confirmar(request);
        return ResponseEntity.ok(new MensajeResponse("La contraseña se cambió correctamente; ya puede iniciar sesión"));
    }
}
