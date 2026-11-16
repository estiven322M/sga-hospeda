import type { FechaISO } from './comun';
import type { EstadoReserva } from './reserva';

/**
 * Estado de una noche en el calendario (7.1, 7.5). Recepción y administración ven LIBRE, RESERVADA o BLOQUEADA; el
 * huésped solo ve LIBRE o NO_DISPONIBLE.
 */
export type EstadoNocheVisible = 'LIBRE' | 'RESERVADA' | 'BLOQUEADA' | 'NO_DISPONIBLE';

export interface NocheCalendarioResponse {
  noche: FechaISO;
  estado: EstadoNocheVisible;
  /** Solo para recepción y administración, en las noches reservadas. */
  codigoReserva: string | null;
  estadoReserva: EstadoReserva | null;
}

/** GET /apartamentos/{identificacion}/calendario?desde&hasta: las noches [desde, hasta), de máximo 62. */
export interface CalendarioResponse {
  identificacionApartamento: string;
  desde: FechaISO;
  hasta: FechaISO;
  noches: NocheCalendarioResponse[];
}
