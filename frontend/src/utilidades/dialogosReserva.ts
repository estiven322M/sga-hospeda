import { confirmarAccion, pedirDato } from './notificaciones';

/** Longitud máxima del motivo de una cancelación (CancelarReservaRequest). */
const MAXIMO_MOTIVO = 500;

/**
 * Pide confirmación para cancelar una reserva y, si la persona la da, el motivo. Devuelve el motivo, o null si se
 * arrepintió. El texto avisa lo que va a pasar: la política de cancelación que quedó congelada al crear la reserva
 * decide cuánto se retiene; no se negocia ni se recalcula con la política vigente (RN-13).
 */
export async function solicitarCancelacion(): Promise<string | null> {
  const confirmada = await confirmarAccion(
    'Cancelar la reserva',
    'Se aplicará la política de cancelación congelada en la reserva al crearla: según la antelación con que se cancele, ' +
      'se retendrá una parte del valor, se revertirá el alojamiento en el folio y las noches quedarán libres. No se puede deshacer.',
  );
  if (!confirmada) return null;
  return pedirDato({
    titulo: 'Motivo de la cancelación',
    texto: 'Escriba el motivo; queda en el historial de la reserva.',
    tipo: 'textarea',
    etiqueta: 'Motivo',
    confirmar: 'Cancelar la reserva',
    maximo: MAXIMO_MOTIVO,
  });
}
