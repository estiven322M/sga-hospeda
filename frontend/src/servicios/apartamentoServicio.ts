import type { ApartamentoResponse, Caracteristica } from '../modelos/apartamento';
import { api } from './api';

/** GET /apartamentos/{identificacion}. 404 si no existe. `signal` permite cancelar la petición al salir de la página. */
export async function obtenerApartamento(identificacion: string, signal?: AbortSignal): Promise<ApartamentoResponse> {
  const respuesta = await api.get<ApartamentoResponse>(`/apartamentos/${encodeURIComponent(identificacion)}`, { signal });
  return respuesta.data;
}

/** GET /caracteristicas: el catálogo cerrado (L-06), sin paginar. Los filtros pintan lo que responde el backend. */
export async function listarCaracteristicas(signal?: AbortSignal): Promise<Caracteristica[]> {
  const respuesta = await api.get<Caracteristica[]>('/caracteristicas', { signal });
  return respuesta.data;
}
