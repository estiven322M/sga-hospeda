import type { BloqueoResponse, RegistrarBloqueoRequest } from '../modelos/apartamento';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

const ruta = (identificacion: string) => `/apartamentos/${encodeURIComponent(identificacion)}/bloqueos`;

/**
 * POST /apartamentos/{identificacion}/bloqueos (CU-09, ADMINISTRADOR): bloquea las noches [fechaInicio, fechaFin).
 * 409 si alguna de esas noches tiene una reserva activa.
 */
export async function registrarBloqueo(identificacion: string, solicitud: RegistrarBloqueoRequest): Promise<BloqueoResponse> {
  const respuesta = await api.post<BloqueoResponse>(ruta(identificacion), solicitud);
  return respuesta.data;
}

/** GET /apartamentos/{identificacion}/bloqueos: de 10 en 10. */
export async function listarBloqueos(identificacion: string, pagina: number, signal?: AbortSignal): Promise<PaginaResponse<BloqueoResponse>> {
  const respuesta = await api.get<PaginaResponse<BloqueoResponse>>(ruta(identificacion), { params: { pagina }, signal });
  return respuesta.data;
}
