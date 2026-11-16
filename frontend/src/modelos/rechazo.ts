import type { FechaHoraISO, FechaISO } from './comun';
import type { CanalOrigen } from './reserva';

/** GET /reservas-externas/rechazos: constancia de una reserva externa rechazada por conflicto (RN-18). */
export interface RechazoReservaExternaResponse {
  canalOrigen: CanalOrigen;
  identificadorExterno: string;
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  /** La regla que provocó el rechazo, tal cual la explicó el backend. */
  motivo: string;
  rechazadaEn: FechaHoraISO;
}
