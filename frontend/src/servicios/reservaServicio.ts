import type { FechaISO } from '../modelos/comun';
import type { PaginaResponse } from '../modelos/paginacion';
import type {
  AutorRequest,
  CancelarReservaRequest,
  CanalOrigen,
  ConfirmarReservaRequest,
  CrearReservaRequest,
  EstadoReserva,
  ModificarReservaRequest,
  ReservaDetalleResponse,
  ReservaResumenResponse,
} from '../modelos/reserva';
import { api } from './api';

const ruta = (codigo: string) => `/reservas/${encodeURIComponent(codigo)}`;

/**
 * POST /reservas (CU-03): responde 201 con la reserva creada. El huésped solo puede reservar por PORTAL y a su
 * nombre (el documento del titular debe ser el de su token; si no, 403); recepción, por DIRECTO. Las reglas del
 * negocio (RP-01, RP-03, disponibilidad...) responden 409 con su mensaje.
 */
export async function crearReserva(solicitud: CrearReservaRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.post<ReservaDetalleResponse>('/reservas', solicitud);
  return respuesta.data;
}

/** GET /reservas/mias (solo HUESPED): las del titular del token, de 10 en 10 y de la más reciente a la más antigua. */
export async function listarMisReservas(
  estado: EstadoReserva | '',
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<ReservaResumenResponse>> {
  const respuesta = await api.get<PaginaResponse<ReservaResumenResponse>>('/reservas/mias', {
    params: { ...(estado !== '' && { estado }), pagina },
    signal,
  });
  return respuesta.data;
}

/** Filtros de GET /reservas (recepción). Lo vacío no se envía. */
export interface FiltrosListadoReservas {
  estado: EstadoReserva | '';
  nombreTitular: string;
  apartamento: string;
  canal: CanalOrigen | '';
  entradaDesde: FechaISO | '';
  entradaHasta: FechaISO | '';
  salidaDesde: FechaISO | '';
  salidaHasta: FechaISO | '';
}

/** GET /reservas (RECEPCIONISTA y ADMINISTRADOR), de 10 en 10 y de la más reciente a la más antigua. */
export async function listarReservas(
  filtros: FiltrosListadoReservas,
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<ReservaResumenResponse>> {
  const params: Record<string, string | number> = { pagina };
  for (const [nombre, valor] of Object.entries(filtros)) {
    if (valor !== '') params[nombre] = valor.trim();
  }
  const respuesta = await api.get<PaginaResponse<ReservaResumenResponse>>('/reservas', { params, signal });
  return respuesta.data;
}

/** GET /reservas/{codigo}. El huésped solo puede verla si es el titular (si no, 403). */
export async function obtenerReserva(codigo: string, signal?: AbortSignal): Promise<ReservaDetalleResponse> {
  const respuesta = await api.get<ReservaDetalleResponse>(ruta(codigo), { signal });
  return respuesta.data;
}

/** PUT /reservas/{codigo}/cancelar (CU-05): aplica la política congelada en la reserva y registra la retención en el folio. */
export async function cancelarReserva(codigo: string, solicitud: CancelarReservaRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.put<ReservaDetalleResponse>(`${ruta(codigo)}/cancelar`, solicitud);
  return respuesta.data;
}

/** PUT /reservas/{codigo}/confirmar. 409 si falta el anticipo o la hora estimada de llegada. */
export async function confirmarReserva(codigo: string, solicitud: ConfirmarReservaRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.put<ReservaDetalleResponse>(`${ruta(codigo)}/confirmar`, solicitud);
  return respuesta.data;
}

/** PUT /reservas/{codigo}/modificar (CU-04): recalcula el valor y registra la diferencia como ajuste en el folio. */
export async function modificarReserva(codigo: string, solicitud: ModificarReservaRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.put<ReservaDetalleResponse>(`${ruta(codigo)}/modificar`, solicitud);
  return respuesta.data;
}

/** PUT /reservas/{codigo}/registrar-llegada (CU-06). */
export async function registrarLlegada(codigo: string, solicitud: AutorRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.put<ReservaDetalleResponse>(`${ruta(codigo)}/registrar-llegada`, solicitud);
  return respuesta.data;
}

/** PUT /reservas/{codigo}/registrar-salida. Exige el folio cerrado. */
export async function registrarSalida(codigo: string, solicitud: AutorRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.put<ReservaDetalleResponse>(`${ruta(codigo)}/registrar-salida`, solicitud);
  return respuesta.data;
}

/** PUT /reservas/{codigo}/declarar-no-show: solo desde la hora límite del día de entrada. */
export async function declararNoShow(codigo: string, solicitud: AutorRequest): Promise<ReservaDetalleResponse> {
  const respuesta = await api.put<ReservaDetalleResponse>(`${ruta(codigo)}/declarar-no-show`, solicitud);
  return respuesta.data;
}
