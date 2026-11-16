import type { FechaHoraISO } from './comun';

export type TipoNotificacion = 'RESERVA_CREADA' | 'RESERVA_CONFIRMADA' | 'RESERVA_CANCELADA' | 'CODIGO_RECUPERACION';

/** ENVIADA (salió), FALLIDA (el proveedor falló) o SIN_CORREO (no se conocía ningún correo del titular). */
export type EstadoNotificacion = 'ENVIADA' | 'FALLIDA' | 'SIN_CORREO';

/**
 * GET /notificaciones (ADMINISTRADOR): un renglón de la bandeja de avisos por correo. `destinatario` es nulo cuando no había
 * correo (SIN_CORREO); `codigoReserva` es nulo en el aviso de recuperación; `detalle` explica el fallo o la ausencia de correo.
 */
export interface NotificacionResponse {
  tipo: TipoNotificacion;
  destinatario: string | null;
  asunto: string;
  /** El contenido del aviso; con el proveedor real, el código de recuperación aparece como "(contenido sensible omitido)". */
  mensaje: string;
  codigoReserva: string | null;
  estado: EstadoNotificacion;
  detalle: string | null;
  registradaEn: FechaHoraISO;
}
