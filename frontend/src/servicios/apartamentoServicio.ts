import type {
  ActualizarApartamentoRequest,
  ApartamentoActualizadoResponse,
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
/** Una subida de varias imágenes puede tardar más que las consultas (10 s): se le da más tiempo. */
const TIEMPO_SUBIDA_MS = 120_000;

/**
 * POST /apartamentos/{id}/imagenes (ADMINISTRADOR, multipart): sube hasta 10 imágenes (JPG, PNG o WEBP, de hasta 5 MB). 201 con el
 * apartamento y su galería. No se fija Content-Type: el navegador arma multipart/form-data con su "boundary" (Guía 21).
 * 409: formato, peso o máximo 10. 503: el almacén de imágenes no respondió (no se registró ninguna).
 */
export async function subirImagenes(
  identificacion: string,
  archivos: readonly File[],
  alProgreso?: (fraccion: number) => void,
): Promise<ApartamentoResponse> {
  const datos = new FormData();
  for (const archivo of archivos) datos.append('imagenes', archivo);
  const respuesta = await api.post<ApartamentoResponse>(`/apartamentos/${encodeURIComponent(identificacion)}/imagenes`, datos, {
    timeout: TIEMPO_SUBIDA_MS,
    onUploadProgress: (evento) => alProgreso?.(evento.progress ?? 0),
  });
  return respuesta.data;
}

/** PUT .../imagenes/{idImagen}/principal (ADMINISTRADOR): la elegida pasa a ser la principal; siempre hay exactamente una. */
export async function establecerImagenPrincipal(identificacion: string, idImagen: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(
    `/apartamentos/${encodeURIComponent(identificacion)}/imagenes/${encodeURIComponent(idImagen)}/principal`, null);
  return respuesta.data;
}

/** PUT .../imagenes/{idImagen}/retirar (ADMINISTRADOR): retiro lógico. 409 si es la última de un apartamento activo. */
export async function retirarImagen(identificacion: string, idImagen: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(
    `/apartamentos/${encodeURIComponent(identificacion)}/imagenes/${encodeURIComponent(idImagen)}/retirar`, null);
  return respuesta.data;
}

export async function crearApartamento(solicitud: CrearApartamentoRequest): Promise<ApartamentoResponse> {
  const respuesta = await api.post<ApartamentoResponse>('/apartamentos', solicitud);
  return respuesta.data;
}

/**
 * PUT /apartamentos/{identificacion}: reemplaza los datos editables; la identificación no cambia. Si se baja la capacidad, la
 * respuesta trae las reservas que quedan por encima (7.3).
 */
export async function actualizarApartamento(identificacion: string, solicitud: ActualizarApartamentoRequest): Promise<ApartamentoActualizadoResponse> {
  const respuesta = await api.put<ApartamentoActualizadoResponse>(ruta(identificacion), solicitud);
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

/** PUT .../estado-operativo/iniciar-preparacion (CU-10): de PENDIENTE_PREPARACION a EN_PREPARACION; 409 desde otro estado. */
export async function iniciarPreparacion(identificacion: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(`${ruta(identificacion)}/estado-operativo/iniciar-preparacion`, null);
  return respuesta.data;
}

/** PUT .../estado-operativo/marcar-preparado (CU-10): de EN_PREPARACION a PREPARADO; 409 desde otro estado. */
export async function marcarPreparado(identificacion: string): Promise<ApartamentoResponse> {
  const respuesta = await api.put<ApartamentoResponse>(`${ruta(identificacion)}/estado-operativo/marcar-preparado`, null);
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
