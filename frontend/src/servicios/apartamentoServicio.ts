import type {
  ActualizarApartamentoRequest,
  ApartamentoResponse,
  Caracteristica,
  CrearApartamentoRequest,
  EstadoOperativo,
} from '../modelos/apartamento';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

const ruta = (identificacion: string) => `/apartamentos/${encodeURIComponent(identificacion)}`;

/** GET /apartamentos/{identificacion}. 404 si no existe. `signal` permite cancelar la petición al salir de la página. */
export async function obtenerApartamento(identificacion: string, signal?: AbortSignal): Promise<ApartamentoResponse> {
  const respuesta = await api.get<ApartamentoResponse>(ruta(identificacion), { signal });
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

/** Filtros de GET /apartamentos. Lo vacío no se envía; con varias características se piden TODAS (L-06). */
export interface FiltrosApartamentos {
  estadoOperativo: EstadoOperativo | '';
  caracteristicas: Caracteristica[];
}

/** GET /apartamentos con filtros, de 10 en 10. Las características viajan repetidas: caracteristicas=A&caracteristicas=B. */
export async function listarApartamentosFiltrados(
  filtros: FiltrosApartamentos,
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<ApartamentoResponse>> {
  const respuesta = await api.get<PaginaResponse<ApartamentoResponse>>('/apartamentos', {
    params: {
      pagina,
      ...(filtros.estadoOperativo !== '' && { estadoOperativo: filtros.estadoOperativo }),
      ...(filtros.caracteristicas.length > 0 && { caracteristicas: filtros.caracteristicas }),
    },
    // Sin esto Axios envía caracteristicas[]=A; el backend espera el nombre repetido
    paramsSerializer: { indexes: null },
    signal,
  });
  return respuesta.data;
}

/** POST /apartamentos (ADMINISTRADOR): nace inactivo hasta tener tarifa en todas las temporadas. 409 si la identificación existe. */
export async function crearApartamento(solicitud: CrearApartamentoRequest): Promise<ApartamentoResponse> {
  const respuesta = await api.post<ApartamentoResponse>('/apartamentos', solicitud);
  return respuesta.data;
}

/** PUT /apartamentos/{identificacion}: reemplaza los datos editables; la identificación no cambia. */
export async function actualizarApartamento(identificacion: string, solicitud: ActualizarApartamentoRequest): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(ruta(identificacion), solicitud);
  return respuesta.data;
}

/** PUT /apartamentos/{identificacion}/activar: 409 si falta la tarifa de alguna temporada (7.4). */
export async function activarApartamento(identificacion: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(`${ruta(identificacion)}/activar`, null);
  return respuesta.data;
}

/** PUT /apartamentos/{identificacion}/baja: baja lógica, el registro nunca se elimina. */
export async function darDeBajaApartamento(identificacion: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(`${ruta(identificacion)}/baja`, null);
  return respuesta.data;
}

/** PUT .../estado-operativo/declarar-fuera-de-servicio: decisión administrativa, desde cualquier estado no ocupado. */
export async function declararFueraDeServicio(identificacion: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(`${ruta(identificacion)}/estado-operativo/declarar-fuera-de-servicio`, null);
  return respuesta.data;
}

/** PUT .../estado-operativo/reincorporar: de FUERA_DE_SERVICIO a PENDIENTE_PREPARACION. */
export async function reincorporarApartamento(identificacion: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(`${ruta(identificacion)}/estado-operativo/reincorporar`, null);
  return respuesta.data;
}
