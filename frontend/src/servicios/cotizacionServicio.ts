import type { CotizacionResponse, CotizarEstanciaRequest } from '../modelos/cotizacion';
import { api } from './api';

/** POST /cotizaciones (CU-01): no crea nada, responde 200. 409 si no cumple la estancia mínima (RP-01). */
export async function cotizarEstancia(solicitud: CotizarEstanciaRequest, signal?: AbortSignal): Promise<CotizacionResponse> {
  const respuesta = await api.post<CotizacionResponse>('/cotizaciones', solicitud, { signal });
  return respuesta.data;
}
