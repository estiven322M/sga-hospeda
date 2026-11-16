import { z } from 'zod';
import { hoyISO } from '../utilidades/fechas';
import { esquemaOcupante } from './ocupante';

const FECHA = /^\d{4}-\d{2}-\d{2}$/;

/** Campo de fecha "AAAA-MM-DD" obligatorio. */
export const fechaObligatoria = (mensaje: string) => z.string().regex(FECHA, mensaje);

interface ConFechas {
  fechaEntrada: string;
  fechaSalida: string;
}

/**
 * Reglas de FORMA de una estancia, compartidas por la búsqueda y la reserva: la entrada no puede estar en el pasado
 * y la salida debe ser posterior a la entrada. Si el negocio la rechaza (estancia mínima por temporada, RP-01), el
 * backend responde 409 y se muestra su mensaje.
 */
export function validarEstancia(datos: ConFechas, contexto: z.RefinementCtx): void {
  if (FECHA.test(datos.fechaEntrada) && datos.fechaEntrada < hoyISO()) {
    contexto.addIssue({ code: 'custom', path: ['fechaEntrada'], message: 'La fecha de entrada no puede estar en el pasado' });
  }
  if (FECHA.test(datos.fechaEntrada) && FECHA.test(datos.fechaSalida) && datos.fechaSalida <= datos.fechaEntrada) {
    contexto.addIssue({ code: 'custom', path: ['fechaSalida'], message: 'La salida debe ser posterior a la entrada' });
  }
}

/** Estancia y grupo: lo que comparten la búsqueda, la cotización y la reserva. Sin refinamientos, para poder extenderlo. */
export const estanciaConGrupo = z.object({
  fechaEntrada: fechaObligatoria('Elija la fecha de entrada'),
  fechaSalida: fechaObligatoria('Elija la fecha de salida'),
  ocupantes: z.array(esquemaOcupante).min(1, 'Agregue al menos un ocupante'),
});

/** Lo necesario para cotizar con los filtros de la búsqueda: fechas válidas y un grupo completo. */
export const esquemaCotizacion = estanciaConGrupo.superRefine(validarEstancia);
