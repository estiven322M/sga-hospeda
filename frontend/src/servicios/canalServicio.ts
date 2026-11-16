import type {
  CanalVentaCreadoResponse,
  CanalVentaResponse,
  CrearCanalVentaRequest,
  EventoCanalResponse,
} from '../modelos/canal';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

/** GET /canales (ADMINISTRADOR): de 10 en 10 por código. Nunca incluye la credencial. */
export async function listarCanales(pagina: number, signal?: AbortSignal): Promise<PaginaResponse<CanalVentaResponse>> {
  const respuesta = await api.get<PaginaResponse<CanalVentaResponse>>('/canales', { params: { pagina }, signal });
  return respuesta.data;
}

/** Todos los canales, recorriendo las páginas de 10 (son pocos): para el filtro de la bitácora. */
export async function listarTodosLosCanales(signal?: AbortSignal): Promise<CanalVentaResponse[]> {
  const primera = await listarCanales(0, signal);
  const canales = [...primera.contenido];
  for (let pagina = 1; pagina < primera.totalPaginas; pagina++) {
    canales.push(...(await listarCanales(pagina, signal)).contenido);
  }
  return canales;
}

/** POST /canales (ADMINISTRADOR): 201 con la credencial en claro, la única vez que se muestra. 409 si el código ya existe. */
export async function crearCanal(solicitud: CrearCanalVentaRequest): Promise<CanalVentaCreadoResponse> {
  const respuesta = await api.post<CanalVentaCreadoResponse>('/canales', solicitud);
  return respuesta.data;
}

/** PUT /canales/{codigo}/desactivar (ADMINISTRADOR): baja lógica; su credencial deja de servir. 409 si ya estaba desactivado. */
export async function desactivarCanal(codigo: string): Promise<CanalVentaResponse> {
  const respuesta = await api.put<CanalVentaResponse>(`/canales/${encodeURIComponent(codigo)}/desactivar`);
  return respuesta.data;
}

/** PUT /canales/{codigo}/activar (ADMINISTRADOR): su misma credencial vuelve a servir. 409 si ya estaba activo. */
export async function activarCanal(codigo: string): Promise<CanalVentaResponse> {
  const respuesta = await api.put<CanalVentaResponse>(`/canales/${encodeURIComponent(codigo)}/activar`);
  return respuesta.data;
}

/** GET /canales/bitacora (ADMINISTRADOR): los mensajes del canal, del más reciente al más antiguo, de 10 en 10. 409 si desde > hasta. */
export async function consultarBitacora(
  canal: string,
  desde: string,
  hasta: string,
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<EventoCanalResponse>> {
  const respuesta = await api.get<PaginaResponse<EventoCanalResponse>>('/canales/bitacora', {
    params: { canal: canal || undefined, desde: desde || undefined, hasta: hasta || undefined, pagina },
    signal,
  });
  return respuesta.data;
}
