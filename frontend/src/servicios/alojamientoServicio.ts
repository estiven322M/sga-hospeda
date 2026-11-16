import type { ActualizarAlojamientoRequest, AlojamientoResponse, ParametrosResponse } from '../modelos/alojamiento';
import { api } from './api';

/** GET /alojamiento: PÚBLICO (lo usan Inicio, el detalle de un apartamento y los formularios de pago). */
export async function obtenerAlojamiento(signal?: AbortSignal): Promise<AlojamientoResponse> {
  const respuesta = await api.get<AlojamientoResponse>('/alojamiento', { signal });
  return respuesta.data;
}

/** PUT /alojamiento (ADMINISTRADOR). 409 si el dominio rechaza algún dato (p. ej. la salida no es anterior a la entrada). */
export async function actualizarAlojamiento(solicitud: ActualizarAlojamientoRequest): Promise<AlojamientoResponse> {
  const respuesta = await api.put<AlojamientoResponse>('/alojamiento', solicitud);
  return respuesta.data;
}

/** GET /alojamiento/parametros (ADMINISTRADOR): configuración del sistema, de solo lectura. */
export async function obtenerParametros(signal?: AbortSignal): Promise<ParametrosResponse> {
  const respuesta = await api.get<ParametrosResponse>('/alojamiento/parametros', { signal });
  return respuesta.data;
}
