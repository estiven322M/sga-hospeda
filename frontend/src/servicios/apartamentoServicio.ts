import type { PaginaResponse } from '../modelos/paginacion';
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

/** GET /apartamentos: una página de 10 (activos o no). */
export async function listarApartamentos(pagina: number, signal?: AbortSignal): Promise<PaginaResponse<ApartamentoResponse>> {
  const respuesta = await api.get<PaginaResponse<ApartamentoResponse>>('/apartamentos', { params: { pagina }, signal });
  return respuesta.data;
}

/**
 * Todos los apartamentos, pidiendo página por página al servidor. Sirve para llenar un <select> (el catálogo es
 * pequeño); los listados que se muestran al usuario SÍ se paginan de 10 en 10.
 */
export async function listarTodosLosApartamentos(signal?: AbortSignal): Promise<ApartamentoResponse[]> {
  const todos: ApartamentoResponse[] = [];
  let pagina = 0;
  let totalPaginas = 1;
  while (pagina < totalPaginas) {
    const respuesta = await listarApartamentos(pagina, signal);
    todos.push(...respuesta.contenido);
    totalPaginas = respuesta.totalPaginas;
    pagina += 1;
  }
  return todos;
}
