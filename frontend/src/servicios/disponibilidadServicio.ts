import type { ApartamentoDisponibleResponse, ConsultarDisponibilidadRequest } from '../modelos/cotizacion';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

/**
 * POST /disponibilidad (CU-02). Es una consulta (responde 200); se usa POST porque el grupo viaja en el cuerpo.
 * `pagina` va en la URL (desde 0): la página de 10 la arma el servidor. 409 si la estancia no cumple la estancia
 * mínima de su temporada (RP-01).
 */
export async function consultarDisponibilidad(
  solicitud: ConsultarDisponibilidadRequest,
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<ApartamentoDisponibleResponse>> {
  const respuesta = await api.post<PaginaResponse<ApartamentoDisponibleResponse>>('/disponibilidad', solicitud, {
    params: { pagina },
    signal,
  });
  return respuesta.data;
}
