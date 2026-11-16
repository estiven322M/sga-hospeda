import type { PaginaResponse } from '../modelos/paginacion';
import type {
  RegistrarTarifaRequest,
  RegistrarTemporadaRequest,
  TarifaResponse,
  TemporadaResponse,
} from '../modelos/tarifario';
import { api } from './api';

/** Recorre todas las páginas de un listado del servidor (para cruzar datos y llenar selectores; los listados visibles se paginan). */
async function todasLasPaginas<T>(pedir: (pagina: number) => Promise<PaginaResponse<T>>): Promise<T[]> {
  const todos: T[] = [];
  let pagina = 0;
  let totalPaginas = 1;
  while (pagina < totalPaginas) {
    const respuesta = await pedir(pagina);
    todos.push(...respuesta.contenido);
    totalPaginas = respuesta.totalPaginas;
    pagina += 1;
  }
  return todos;
}

/** GET /temporadas (RECEPCIONISTA y ADMINISTRADOR): de 10 en 10. */
export async function listarTemporadas(pagina: number, signal?: AbortSignal): Promise<PaginaResponse<TemporadaResponse>> {
  const respuesta = await api.get<PaginaResponse<TemporadaResponse>>('/temporadas', { params: { pagina }, signal });
  return respuesta.data;
}

export function listarTodasLasTemporadas(signal?: AbortSignal): Promise<TemporadaResponse[]> {
  return todasLasPaginas((pagina) => listarTemporadas(pagina, signal));
}

/** POST /temporadas (ADMINISTRADOR): 409 si se solapa con otra o si ya existe una base. */
export async function registrarTemporada(solicitud: RegistrarTemporadaRequest): Promise<TemporadaResponse> {
  const respuesta = await api.post<TemporadaResponse>('/temporadas', solicitud);
  return respuesta.data;
}

const rutaTarifas = (identificacion: string) => `/apartamentos/${encodeURIComponent(identificacion)}/tarifas`;

/** GET /apartamentos/{identificacion}/tarifas: de 10 en 10. */
export async function listarTarifas(identificacion: string, pagina: number, signal?: AbortSignal): Promise<PaginaResponse<TarifaResponse>> {
  const respuesta = await api.get<PaginaResponse<TarifaResponse>>(rutaTarifas(identificacion), { params: { pagina }, signal });
  return respuesta.data;
}

export function listarTodasLasTarifas(identificacion: string, signal?: AbortSignal): Promise<TarifaResponse[]> {
  return todasLasPaginas((pagina) => listarTarifas(identificacion, pagina, signal));
}

/** PUT /apartamentos/{identificacion}/tarifas/{temporada} (ADMINISTRADOR): define o reemplaza la tarifa (idempotente). */
export async function registrarTarifa(identificacion: string, temporada: string, solicitud: RegistrarTarifaRequest): Promise<TarifaResponse> {
  const respuesta = await api.put<TarifaResponse>(`${rutaTarifas(identificacion)}/${encodeURIComponent(temporada)}`, solicitud);
  return respuesta.data;
}
