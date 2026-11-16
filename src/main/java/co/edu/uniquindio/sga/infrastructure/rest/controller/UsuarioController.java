package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ImagenSubidaMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import co.edu.uniquindio.sga.application.dto.request.ActualizarPerfilRequest;
import co.edu.uniquindio.sga.application.dto.request.CambiarContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.CambiarRolRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearUsuarioInternoRequest;
import co.edu.uniquindio.sga.application.dto.response.MensajeResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioResponse;
import co.edu.uniquindio.sga.application.security.GestionUsuariosService;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.Paginas;
import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Adaptador REST de la gestión de usuarios (7.2). Delgado: valida la forma y delega en el puerto de aplicación.
 * <ul>
 *   <li>Administración (solo ADMINISTRADOR): listar, crear usuarios internos, cambiar el rol y desactivar (lógico).</li>
 *   <li>Perfil propio ({@code /yo}, cualquier rol): consultar y editar nombre y teléfono, y cambiar la contraseña. A
 *       quién se refiere {@code /yo} sale del token (el sujeto es el correo), nunca de un parámetro: nadie puede
 *       editar el perfil de otro por esta vía.</li>
 * </ul>
 * No hay DELETE: la eliminación es lógica. {@code /yo} se declara antes que {@code /{id}} para que no se confunda con un id.
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios, roles y perfil")
public class UsuarioController {

    private static final String CUALQUIER_ROL = "hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO')";

    private final GestionUsuariosService usuarios;
    private final ImagenSubidaMapper imagenSubida;

    public UsuarioController(GestionUsuariosService usuarios, ImagenSubidaMapper imagenSubida) {
        this.usuarios = usuarios;
        this.imagenSubida = imagenSubida;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar usuarios, de 10 en 10, con filtro opcional por rol")
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        var resultado = usuarios.listar(rol == null ? null : rol.name(), new SolicitudPagina(pagina));
        return ResponseEntity.ok(Paginas.aResponse(resultado, usuario -> usuario));
    }

    /** 201 con Location. Crea un usuario interno (recepcionista, administrador o personal de servicio). */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear un usuario interno con su rol")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioInternoRequest request) {
        UsuarioResponse creado = usuarios.crearInterno(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/yo")
    @PreAuthorize(CUALQUIER_ROL)
    @Operation(summary = "Consultar mi perfil")
    public ResponseEntity<UsuarioResponse> miPerfil(Authentication authentication) {
        return ResponseEntity.ok(usuarios.perfil(authentication.getName()));
    }

    @PutMapping("/yo")
    @PreAuthorize(CUALQUIER_ROL)
    @Operation(summary = "Actualizar mi nombre y mi teléfono (el correo no se edita)")
    public ResponseEntity<UsuarioResponse> actualizarMiPerfil(@Valid @RequestBody ActualizarPerfilRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(usuarios.actualizarPerfil(authentication.getName(), request));
    }

    /**
     * 7.2: foto de perfil (multipart, parte "foto"; una imagen JPG, PNG o WEBP de hasta 2 MB), guardada con el mismo puerto de
     * almacenamiento que las imágenes de los apartamentos. Es siempre la del dueño del token. 409 si pesa de más o no es una
     * imagen admitida; 503 si el almacén externo no pudo guardarla (la foto anterior se conserva).
     */
    @PutMapping(value = "/yo/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CUALQUIER_ROL)
    @Operation(summary = "Cambiar mi foto de perfil (multipart)")
    public ResponseEntity<UsuarioResponse> cambiarMiFoto(@RequestParam("foto") MultipartFile foto, Authentication authentication) {
        return ResponseEntity.ok(usuarios.actualizarFoto(authentication.getName(), imagenSubida.aContenido(foto)));
    }

    @PutMapping("/yo/contrasena")
    @PreAuthorize(CUALQUIER_ROL)
    @Operation(summary = "Cambiar mi contraseña con la actual y la nueva")
    public ResponseEntity<MensajeResponse> cambiarMiContrasena(@Valid @RequestBody CambiarContrasenaRequest request,
                                                               Authentication authentication) {
        usuarios.cambiarContrasena(authentication.getName(), request);
        return ResponseEntity.ok(new MensajeResponse("La contraseña se cambió correctamente"));
    }

    @PutMapping("/{id}/rol")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Cambiar el rol de un usuario")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable long id, @Valid @RequestBody CambiarRolRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(usuarios.cambiarRol(id, request.rol(), authentication.getName()));
    }

    /** Eliminación lógica: el usuario no podrá iniciar sesión, pero su titular y sus reservas históricas se conservan. */
    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Desactivar un usuario (eliminación lógica)")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable long id, Authentication authentication) {
        return ResponseEntity.ok(usuarios.desactivar(id, authentication.getName()));
    }
}
