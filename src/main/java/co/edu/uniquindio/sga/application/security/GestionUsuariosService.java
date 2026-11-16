package co.edu.uniquindio.sga.application.security;

import co.edu.uniquindio.sga.application.dto.request.ActualizarPerfilRequest;
import co.edu.uniquindio.sga.application.dto.request.CambiarContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearUsuarioInternoRequest;
import co.edu.uniquindio.sga.application.dto.response.UsuarioResponse;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/**
 * Puerto de la gestión de usuarios (7.2). El usuario no es del dominio (3.6): la implementación vive en infraestructura
 * junto a su entidad. La única regla que viene del negocio (no desactivar a un titular con reservas activas) la consulta
 * al dominio por documento. El rol viaja como texto (HUESPED, RECEPCIONISTA, ADMINISTRADOR, PERSONAL_SERVICIO).
 * Quien pide (el administrador o el dueño del perfil) se identifica por su correo, que es el sujeto del JWT.
 */
public interface GestionUsuariosService {

    /** De 10 en 10 por nombre, con filtro opcional por rol. */
    Pagina<UsuarioResponse> listar(String rol, SolicitudPagina pagina);

    /** Crea un usuario interno. Correo o documento ya registrados: 409. */
    UsuarioResponse crearInterno(CrearUsuarioInternoRequest solicitud);

    /** Cambia el rol. Un administrador no puede cambiar el suyo (409). */
    UsuarioResponse cambiarRol(long id, String rol, String correoSolicitante);

    /** Eliminación lógica. 409 si es uno mismo o es titular de reservas PENDIENTE, CONFIRMADA o EN_CURSO. */
    UsuarioResponse desactivar(long id, String correoSolicitante);

    UsuarioResponse perfil(String correo);

    /**
     * 7.2: cambia la foto de perfil (una imagen JPG, PNG o WEBP de hasta 2 MB) guardándola en el almacén de imágenes. Si el almacén
     * falla se lanza AlmacenamientoImagenesException (503) y la foto anterior se conserva.
     */
    UsuarioResponse actualizarFoto(String correo, co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen foto);

    /** Solo nombre y teléfono: el correo no se edita (7.2). */
    UsuarioResponse actualizarPerfil(String correo, ActualizarPerfilRequest solicitud);

    /** Con la contraseña actual y la nueva. Actual incorrecta o nueva igual a la actual: 409. */
    void cambiarContrasena(String correo, CambiarContrasenaRequest solicitud);
}
