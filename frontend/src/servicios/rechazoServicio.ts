import type { PaginaResponse } from '../modelos/paginacion';
import type { FiltroRevision, RechazoReservaExternaResponse } from '../modelos/rechazo';
import { api } from './api';

/** GET /reservas-externas/rechazos (ADMINISTRADOR): los conflictos del canal externo (RN-18), de 10 en 10, con filtro por revisión. */
export async function listarRechazos(
  filtro: FiltroRevision,
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<RechazoReservaExternaResponse>> {
  const revisado = filtro === 'TODOS' ? undefined : filtro === 'REVISADOS';
  const respuesta = await api.get<PaginaResponse<RechazoReservaExternaResponse>>('/reservas-externas/rechazos', {
    params: { revisado, pagina },
    signal,
  });
  return respuesta.data;
}

/**
 * PUT /reservas-externas/rechazos/{id}/resolver (ADMINISTRADOR): marca el conflicto como revisado con una nota. Nunca toca la
 * reserva vigente. 409 si ya estaba revisado, 404 si no existe.
 */
export async function resolverRechazo(id: number, nota: string): Promise<RechazoReservaExternaResponse> {
  const respuesta = await api.put<RechazoReservaExternaResponse>(`/reservas-externas/rechazos/${id}/resolver`, { nota });
  return respuesta.data;
}
