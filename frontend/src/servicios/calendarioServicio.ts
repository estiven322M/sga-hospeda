import type { FechaISO } from '../modelos/comun';
import type { CalendarioResponse } from '../modelos/calendario';
import { api } from './api';

/**
 * GET /apartamentos/{identificacion}/calendario?desde&hasta: el estado de cada noche del rango [desde, hasta), de
 * máximo 62 noches (409 si se excede). Qué ve cada rol lo decide el backend: recepción y administración, el estado
 * real y el código de la reserva; el huésped, solo LIBRE o NO_DISPONIBLE.
 */
export async function obtenerCalendario(
  identificacion: string,
  desde: FechaISO,
  hasta: FechaISO,
  signal?: AbortSignal,
): Promise<CalendarioResponse> {
  const respuesta = await api.get<CalendarioResponse>(`/apartamentos/${encodeURIComponent(identificacion)}/calendario`, {
    params: { desde, hasta },
    signal,
  });
  return respuesta.data;
}
