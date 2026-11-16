import type { PoliticaCancelacionResponse, RegistrarPoliticaCancelacionRequest } from '../modelos/tarifario';
import { api } from './api';

/** GET /politicas-cancelacion/vigente: la política que rige hoy (fecha del servidor). */
export async function obtenerPoliticaVigente(signal?: AbortSignal): Promise<PoliticaCancelacionResponse> {
  const respuesta = await api.get<PoliticaCancelacionResponse>('/politicas-cancelacion/vigente', { signal });
  return respuesta.data;
}

/** GET /politicas-cancelacion/{version}: una versión cualquiera (las anteriores se conservan siempre). 404 si no existe. */
export async function obtenerPoliticaPorVersion(version: number, signal?: AbortSignal): Promise<PoliticaCancelacionResponse> {
  const respuesta = await api.get<PoliticaCancelacionResponse>(`/politicas-cancelacion/${version}`, { signal });
  return respuesta.data;
}

/**
 * POST /politicas-cancelacion (ADMINISTRADOR): crea la versión siguiente. Las reservas ya creadas conservan la
 * versión que congelaron (RN-13). 409 si no cumple las reglas de la política.
 */
export async function registrarPoliticaCancelacion(solicitud: RegistrarPoliticaCancelacionRequest): Promise<PoliticaCancelacionResponse> {
  const respuesta = await api.post<PoliticaCancelacionResponse>('/politicas-cancelacion', solicitud);
  return respuesta.data;
}
