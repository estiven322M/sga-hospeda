import type { CrearReservaRequest, ReservaDetalleResponse } from '../modelos/reserva';
import { api } from './api';

/**
 * POST /reservas (CU-03): responde 201 con la reserva creada. El huésped solo puede reservar por PORTAL y a su
 * nombre (el documento del titular debe ser el de su token; si no, 403). Las reglas del negocio (RP-01, RP-03,
 * disponibilidad...) responden 409 con su mensaje.
 */
export async function crearReserva(solicitud: CrearReservaRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.post<ReservaDetalleResponse>('/reservas', solicitud);
  return respuesta.data;
}
