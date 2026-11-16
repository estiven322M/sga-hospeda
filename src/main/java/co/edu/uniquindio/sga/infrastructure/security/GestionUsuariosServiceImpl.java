package co.edu.uniquindio.sga.infrastructure.security;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.dto.request.ActualizarPerfilRequest;
import co.edu.uniquindio.sga.application.dto.request.CambiarContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearUsuarioInternoRequest;
import co.edu.uniquindio.sga.application.dto.response.UsuarioResponse;
import co.edu.uniquindio.sga.application.exception.OperacionUsuarioNoPermitidaException;
import co.edu.uniquindio.sga.application.exception.UsuarioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.UsuarioYaRegistradoException;
import co.edu.uniquindio.sga.application.security.GestionUsuariosService;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.service.BajaUsuarioService;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.PaginacionJpa;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;

/**
 * Gestión de usuarios (7.2). El usuario es un concepto de acceso, no del dominio (3.6): esta clase orquesta su entidad
 * de infraestructura. Lo que sí es del negocio (un titular con reservas activas no se desactiva) lo decide el servicio
 * de dominio BajaUsuarioService por documento. Contraseñas siempre con BCrypt; nunca se devuelve el hash.
 */
@Service
@Transactional
public class GestionUsuariosServiceImpl implements GestionUsuariosService {

    private static final Sort POR_NOMBRE = Sort.by("nombre", "id");

    private final UsuarioJpaDataRepository usuarios;
    private final PasswordEncoder encoder;
    private final BajaUsuarioService bajaUsuario;

    public GestionUsuariosServiceImpl(UsuarioJpaDataRepository usuarios, PasswordEncoder encoder,
                                      BajaUsuarioService bajaUsuario) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.bajaUsuario = bajaUsuario;
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<UsuarioResponse> listar(String rol, SolicitudPagina pagina) {
        var solicitud = PaginacionJpa.solicitud(pagina, POR_NOMBRE);
        Page<UsuarioEntity> resultado = rol == null || rol.isBlank()
                ? usuarios.findAll(solicitud)
                : usuarios.findByRol(RolUsuario.valueOf(rol), solicitud);
        return PaginacionJpa.aDominio(resultado, pagina, GestionUsuariosServiceImpl::aRespuesta);
    }

    @Override
    public UsuarioResponse crearInterno(CrearUsuarioInternoRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        String documento = solicitud.documento().trim();
        if (usuarios.existsByCorreo(correo)) {
            throw new UsuarioYaRegistradoException("Ya existe un usuario registrado con ese correo");
        }
        if (usuarios.existsByDocumento(documento)) {
            throw new UsuarioYaRegistradoException("Ya existe un usuario registrado con ese documento");
        }
        return aRespuesta(usuarios.save(new UsuarioEntity(solicitud.nombre().trim(), correo,
                encoder.encode(solicitud.contrasena()), solicitud.telefono().trim(), documento,
                solicitud.fechaNacimiento(), RolUsuario.valueOf(solicitud.rol()))));
    }

    @Override
    public UsuarioResponse cambiarRol(long id, String rol, String correoSolicitante) {
        UsuarioEntity usuario = buscarPorId(id);
        if (esElMismo(usuario, correoSolicitante)) {
            throw new OperacionUsuarioNoPermitidaException("No puede cambiar su propio rol");
        }
        usuario.cambiarRol(RolUsuario.valueOf(rol));
        return aRespuesta(usuario);
    }

    @Override
    public UsuarioResponse desactivar(long id, String correoSolicitante) {
        UsuarioEntity usuario = buscarPorId(id);
        if (esElMismo(usuario, correoSolicitante)) {
            throw new OperacionUsuarioNoPermitidaException("No puede desactivar su propia cuenta");
        }
        if (usuario.isActivo()) {
            // 7.2: la regla del titular con reservas activas la decide el dominio, por documento
            bajaUsuario.verificarPuedeDesactivarse(new DocumentoIdentidad(usuario.getDocumento()));
            usuario.desactivar();
        }
        return aRespuesta(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String correo) {
        return aRespuesta(buscarPorCorreo(correo));
    }

    @Override
    public UsuarioResponse actualizarPerfil(String correo, ActualizarPerfilRequest solicitud) {
        UsuarioEntity usuario = buscarPorCorreo(correo);
        usuario.actualizarPerfil(solicitud.nombre().trim(), solicitud.telefono().trim());
        return aRespuesta(usuario);
    }

    @Override
    public void cambiarContrasena(String correo, CambiarContrasenaRequest solicitud) {
        UsuarioEntity usuario = buscarPorCorreo(correo);
        if (!encoder.matches(solicitud.contrasenaActual(), usuario.getContrasena())) {
            throw new OperacionUsuarioNoPermitidaException("La contraseña actual no es correcta");
        }
        if (encoder.matches(solicitud.contrasenaNueva(), usuario.getContrasena())) {
            throw new OperacionUsuarioNoPermitidaException("La contraseña nueva debe ser distinta de la actual");
        }
        usuario.cambiarContrasena(encoder.encode(solicitud.contrasenaNueva()));
    }

    private UsuarioEntity buscarPorId(long id) {
        return usuarios.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException("con id " + id));
    }

    private UsuarioEntity buscarPorCorreo(String correo) {
        return usuarios.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsuarioNoEncontradoException("con el correo " + correo));
    }

    private static boolean esElMismo(UsuarioEntity usuario, String correoSolicitante) {
        return usuario.getCorreo().equalsIgnoreCase(correoSolicitante);
    }

    private static UsuarioResponse aRespuesta(UsuarioEntity u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getDocumento(),
                u.getFechaNacimiento(), u.getRol().name(), u.isActivo());
    }
}
