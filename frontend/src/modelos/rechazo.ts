import type { FechaHoraISO, FechaISO } from './comun';
import type { CanalOrigen } from './reserva';

/** GET /reservas-externas/rechazos: conflicto de una reserva externa rechazada (RN-18), con su revisión (7.8). */
export interface RechazoReservaExternaResponse {
  /** Identificador del conflicto: sirve para resolverlo. */
  id: number;
  canalOrigen: CanalOrigen;
  /** Código del canal que envió el mensaje; nulo en los rechazos sembrados. */
  canal: string | null;
  identificadorExterno: string;
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  /** La regla que provocó el rechazo, tal cual la explicó el backend. */
  motivo: string;
  rechazadaEn: FechaHoraISO;
  revisado: boolean;
  notaRevision: string | null;
  revisadoPor: string | null;
  revisadoEn: FechaHoraISO | null;
}

/** Filtro del listado: todos, solo los pendientes o solo los ya revisados. */
export type FiltroRevision = 'TODOS' | 'PENDIENTES' | 'REVISADOS';
