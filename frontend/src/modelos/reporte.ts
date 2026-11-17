import type { FechaISO, Pesos } from './comun';
import type { CanalOrigen } from './reserva';

/** Los tres reportes de GET /reportes/{tipo} y de su exportación (7.9, L-19, L-23). */
export type TipoReporte = 'ocupacion' | 'ingresos' | 'reservas-por-canal';

/** GET /reportes/ocupacion: porcentajeOcupacion = nochesVendidas / nochesDisponibles × 100 (lo calcula el backend). */
export interface ReporteOcupacionResponse {
  desde: FechaISO;
  hasta: FechaISO;
  apartamentosActivos: number;
  nochesPeriodo: number;
  nochesDisponibles: number;
  nochesVendidas: number;
  porcentajeOcupacion: number;
}

export interface IngresoPorMedioResponse {
  medio: string;
  total: Pesos;
}

/** GET /reportes/ingresos: pagos registrados en el periodo por medio de pago (los cargos sin pagar no cuentan). */
export interface ReporteIngresosResponse {
  desde: FechaISO;
  hasta: FechaISO;
  porMedio: IngresoPorMedioResponse[];
  total: Pesos;
  moneda: string;
}

export interface ReservasDeCanalResponse {
  canal: CanalOrigen;
  total: number;
  canceladas: number;
  noShow: number;
}

/** GET /reportes/reservas-por-canal: reservas creadas en el periodo; los totales incluyen canceladas y no-show. */
export interface ReporteReservasPorCanalResponse {
  desde: FechaISO;
  hasta: FechaISO;
  canales: ReservasDeCanalResponse[];
  total: number;
  canceladas: number;
  noShow: number;
}
