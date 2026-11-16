import type { PaginaResponse } from '../modelos/paginacion';
import type { RechazoReservaExternaResponse } from '../modelos/rechazo';
import { api } from './api';

/** GET /reservas-externas/rechazos (ADMINISTRADOR): las reservas del canal externo rechazadas por conflicto (RN-18), de 10 en 10. */
export async function listarRechazos(pagina: number, signal?: AbortSignal): Promise<PaginaResponse<RechazoReservaExternaResponse>> {
  const respuesta = await api.get<PaginaResponse<RechazoReservaExternaResponse>>('/reservas-externas/rechazos', { params: { pagina }, signal });
  return respuesta.data;
}
