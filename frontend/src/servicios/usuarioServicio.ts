import type { RolUsuario } from '../modelos/auth';
import type { PaginaResponse } from '../modelos/paginacion';
import type {
  ActualizarPerfilRequest,
  CambiarContrasenaRequest,
  CrearUsuarioInternoRequest,
  MensajeResponse,
  UsuarioResponse,
} from '../modelos/usuario';
import { api } from './api';

/** GET /usuarios (ADMINISTRADOR): de 10 en 10 por nombre, con filtro opcional por rol; incluye a los desactivados. */
export async function listarUsuarios(
  rol: RolUsuario | '',
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<UsuarioResponse>> {
  const respuesta = await api.get<PaginaResponse<UsuarioResponse>>('/usuarios', {
    params: { rol: rol === '' ? undefined : rol, pagina },
    signal,
  });
  return respuesta.data;
}

/** POST /usuarios (ADMINISTRADOR): 201. 409 si el correo o el documento ya están registrados. */
export async function crearUsuarioInterno(solicitud: CrearUsuarioInternoRequest): Promise<UsuarioResponse> {
  const respuesta = await api.post<UsuarioResponse>('/usuarios', solicitud);
  return respuesta.data;
}

/** PUT /usuarios/{id}/rol (ADMINISTRADOR). El rol nuevo rige desde el siguiente inicio de sesión. 409 si es el propio. */
export async function cambiarRolUsuario(id: number, rol: RolUsuario): Promise<UsuarioResponse> {
  const respuesta = await api.put<UsuarioResponse>(`/usuarios/${id}/rol`, { rol });
  return respuesta.data;
}

/** PUT /usuarios/{id}/desactivar (ADMINISTRADOR): eliminación lógica. 409 si es uno mismo o titular de reservas activas. */
export async function desactivarUsuario(id: number): Promise<UsuarioResponse> {
  const respuesta = await api.put<UsuarioResponse>(`/usuarios/${id}/desactivar`, null);
  return respuesta.data;
}

/** GET /usuarios/yo: el perfil de quien tiene la sesión (lo decide el token, nunca un parámetro). */
export async function obtenerMiPerfil(signal?: AbortSignal): Promise<UsuarioResponse> {
  const respuesta = await api.get<UsuarioResponse>('/usuarios/yo', { signal });
  return respuesta.data;
}

/** PUT /usuarios/yo: solo nombre y teléfono. */
export async function actualizarMiPerfil(solicitud: ActualizarPerfilRequest): Promise<UsuarioResponse> {
  const respuesta = await api.put<UsuarioResponse>('/usuarios/yo', solicitud);
  return respuesta.data;
}

/** PUT /usuarios/yo/contrasena. 409 si la actual es incorrecta o la nueva es igual a la actual. */
export async function cambiarMiContrasena(solicitud: CambiarContrasenaRequest): Promise<MensajeResponse> {
  const respuesta = await api.put<MensajeResponse>('/usuarios/yo/contrasena', solicitud);
  return respuesta.data;
}
