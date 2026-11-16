import type { CerrarFolioRequest, FolioResponse, RegistrarPagoRequest } from '../modelos/folio';
import { api } from './api';

const ruta = (codigo: string) => `/reservas/${encodeURIComponent(codigo)}/folio`;

/** GET /reservas/{codigo}/folio. El huésped solo lo ve si es el titular (si no, 403). */
export async function obtenerFolio(codigo: string, signal?: AbortSignal): Promise<FolioResponse> {
  const respuesta = await api.get<FolioResponse>(ruta(codigo), { signal });
  return respuesta.data;
}

/** POST /reservas/{codigo}/folio/pagos (CU-08): devuelve el folio con el saldo actualizado. */
export async function registrarPago(codigo: string, solicitud: RegistrarPagoRequest): Promise<FolioResponse> {
  const respuesta = await api.post<FolioResponse>(`${ruta(codigo)}/pagos`, solicitud);
  return respuesta.data;
}

/**
 * PUT /reservas/{codigo}/folio/cerrar (CU-11). Con saldo pendiente exige `autorizadoPor` y `motivo`, y solo el
 * ADMINISTRADOR puede enviarlos (un recepcionista que los envía recibe 403).
 */
export async function cerrarFolio(codigo: string, solicitud: CerrarFolioRequest): Promise<FolioResponse> {
  const respuesta = await api.put<FolioResponse>(`${ruta(codigo)}/cerrar`, solicitud);
  return respuesta.data;
}
