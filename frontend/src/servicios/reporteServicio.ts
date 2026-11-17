import type { FechaISO } from '../modelos/comun';
import type {
  ReporteIngresosResponse,
  ReporteOcupacionResponse,
  ReporteReservasPorCanalResponse,
  TipoReporte,
} from '../modelos/reporte';
import { api } from './api';

/** GET /reportes/ocupacion (ADMINISTRADOR): noches vendidas y porcentaje del periodo [desde, hasta]. */
export async function consultarOcupacion(desde: FechaISO, hasta: FechaISO, signal?: AbortSignal): Promise<ReporteOcupacionResponse> {
  const respuesta = await api.get<ReporteOcupacionResponse>('/reportes/ocupacion', { params: { desde, hasta }, signal });
  return respuesta.data;
}

/** GET /reportes/ingresos (ADMINISTRADOR): pagos del periodo por medio de pago. */
export async function consultarIngresos(desde: FechaISO, hasta: FechaISO, signal?: AbortSignal): Promise<ReporteIngresosResponse> {
  const respuesta = await api.get<ReporteIngresosResponse>('/reportes/ingresos', { params: { desde, hasta }, signal });
  return respuesta.data;
}

/** GET /reportes/reservas-por-canal (ADMINISTRADOR): reservas creadas en el periodo por canal de origen. */
export async function consultarReservasPorCanal(
  desde: FechaISO,
  hasta: FechaISO,
  signal?: AbortSignal,
): Promise<ReporteReservasPorCanalResponse> {
  const respuesta = await api.get<ReporteReservasPorCanalResponse>('/reportes/reservas-por-canal', { params: { desde, hasta }, signal });
  return respuesta.data;
}

/**
 * GET /reportes/{tipo}/exportar (L-23): baja el reporte como Excel. Se pide con el cliente Axios (no con un enlace)
 * porque el token viaja en la cabecera Authorization; el archivo llega como Blob y se guarda con un enlace temporal.
 */
export async function descargarExcel(tipo: TipoReporte, desde: FechaISO, hasta: FechaISO): Promise<void> {
  const respuesta = await api.get<Blob>(`/reportes/${tipo}/exportar`, {
    params: { desde, hasta, formato: 'xlsx' },
    responseType: 'blob',
  });
  const url = URL.createObjectURL(respuesta.data);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `reporte-${tipo}-${desde}-a-${hasta}.xlsx`;
  enlace.click();
  URL.revokeObjectURL(url);
}
