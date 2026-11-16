import type { FechaHoraISO, FechaISO, HoraISO, Pesos } from './comun';
import type { CargoNocheResponse } from './cotizacion';
import type { OcupanteRequest, OcupanteResponse } from './ocupante';

/**
 * Ciclo de vida de la reserva (sección 8). Uniones de texto y no `enum`: así viajan en el JSON y
 * `erasableSyntaxOnly` no permite `enum`.
 */
export type EstadoReserva =
  | 'PENDIENTE'
  | 'CONFIRMADA'
  | 'EN_CURSO'
  | 'FINALIZADA'
  | 'CANCELADA'
  | 'NO_SHOW';

export const ESTADOS_RESERVA: readonly EstadoReserva[] = [
  'PENDIENTE',
  'CONFIRMADA',
  'EN_CURSO',
  'FINALIZADA',
  'CANCELADA',
  'NO_SHOW',
];

/** Canal de venta. EXTERNO exige `identificadorExterno` y solo lo usa el administrador (RN-19). */
export type CanalOrigen = 'PORTAL' | 'DIRECTO' | 'EXTERNO';

export const CANALES_ORIGEN: readonly CanalOrigen[] = ['PORTAL', 'DIRECTO', 'EXTERNO'];

/** Un hecho del historial de la reserva. */
export interface EventoReservaResponse {
  momento: FechaHoraISO;
  /** Por ejemplo CREACION, CONFIRMACION, MODIFICACION, CANCELACION, REGISTRO, SALIDA, NO_SHOW, VENCIMIENTO. */
  accion: string;
  autor: string;
  /** El backend lo envía null en el primer evento (la creación). */
  estadoAnterior: EstadoReserva | null;
  estadoNuevo: EstadoReserva;
  observacion: string;
}

/**
 * GET /reservas/{codigo} y respuesta de crear, confirmar, modificar, cancelar y de la operación diaria.
 * `valorTotal` es el valor congelado NETO: `subtotalBruto` − `descuento` (RP-02). Es lo que se cobra y se anticipa.
 */
export interface ReservaDetalleResponse {
  /** RES-AAAA-NNNNN. */
  codigo: string;
  estado: EstadoReserva;
  canalOrigen: CanalOrigen;
  /** Solo las reservas del canal EXTERNO lo tienen (RN-19). */
  identificadorExterno: string | null;
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  noches: number;
  /** El yaml no la marca nullable, pero el backend la envía null mientras no se haya informado (es obligatoria para confirmar, RN-09). */
  horaEstimadaLlegada: HoraISO | null;
  titular: OcupanteResponse;
  ocupantes: OcupanteResponse[];
  valorTotal: Pesos;
  subtotalBruto: Pesos;
  porcentajeDescuento: number;
  descuento: Pesos;
  moneda: string;
  desglose: CargoNocheResponse[];
  /** Versión de la política de cancelación congelada al crear (RN-13, RN-22). */
  versionPolitica: number;
  creadaEn: FechaHoraISO;
  historial: EventoReservaResponse[];
}

/** Un renglón de los listados: GET /reservas y GET /reservas/mias. De la más reciente a la más antigua. */
export interface ReservaResumenResponse {
  codigo: string;
  estado: EstadoReserva;
  canalOrigen: CanalOrigen;
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  nombreTitular: string;
  totalOcupantes: number;
  /** Valor neto congelado. */
  valorTotal: Pesos;
  creadaEn: FechaHoraISO;
}

/** POST /reservas (CU-03). El titular debe ser uno de los ocupantes. */
export interface CrearReservaRequest {
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  canalOrigen: CanalOrigen;
  /** Obligatorio con canal EXTERNO y prohibido con los demás (RN-19). Máximo 60 caracteres. */
  identificadorExterno?: string;
  horaEstimadaLlegada?: HoraISO;
  titular: OcupanteRequest;
  /** Al menos 1. Debe incluir un adulto responsable (RP-03). */
  ocupantes: OcupanteRequest[];
}

/** PUT /reservas/{codigo}/modificar (CU-04). Sin `identificacionApartamento` conserva el apartamento actual. */
export interface ModificarReservaRequest {
  identificacionApartamento?: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  ocupantes: OcupanteRequest[];
  autor: string;
}

/** PUT /reservas/{codigo}/confirmar. La hora de llegada es obligatoria para confirmar si aún no se informó (RN-09). */
export interface ConfirmarReservaRequest {
  horaEstimadaLlegada?: HoraISO;
  autor: string;
}

/** PUT /reservas/{codigo}/cancelar (CU-05). */
export interface CancelarReservaRequest {
  /** Máximo 500 caracteres. */
  motivo: string;
  autor: string;
}

/** Cuerpo de registrar-llegada, registrar-salida y declarar-no-show. */
export interface AutorRequest {
  autor: string;
}
