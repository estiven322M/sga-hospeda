import type { NovedadResponse, RegistrarNovedadRequest } from '../modelos/novedad';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

const ruta = (identificacion: string) => `/apartamentos/${encodeURIComponent(identificacion)}/novedades`;

/**
 * POST /apartamentos/{identificacion}/novedades (PERSONAL_SERVICIO, RECEPCIONISTA, ADMINISTRADOR): deja constancia de una
 * novedad. El autor es quien inició sesión y la fecha la pone el servidor. 400 con `errores` si la forma no es válida.
 */
export async function registrarNovedad(identificacion: string, solicitud: RegistrarNovedadRequest): Promise<NovedadResponse> {
  const respuesta = await api.post<NovedadResponse>(ruta(identificacion), solicitud);
  return respuesta.data;
}

/** GET /apartamentos/{identificacion}/novedades: el historial, de 10 en 10, de la más reciente a la más antigua. */
export async function listarNovedades(identificacion: string, pagina: number, signal?: AbortSignal): Promise<PaginaResponse<NovedadResponse>> {
  const respuesta = await api.get<PaginaResponse<NovedadResponse>>(ruta(identificacion), { params: { pagina }, signal });
  return respuesta.data;
}
