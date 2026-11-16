import type { NotificacionResponse } from '../modelos/notificacion';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

/**
 * GET /notificaciones (ADMINISTRADOR): la bandeja de avisos por correo, de 10 en 10 y de la más reciente a la más antigua.
 * Es en memoria: se vacía cuando el servidor se reinicia.
 */
export async function listarNotificaciones(
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<NotificacionResponse>> {
  const respuesta = await api.get<PaginaResponse<NotificacionResponse>>('/notificaciones', { params: { pagina }, signal });
  return respuesta.data;
}
