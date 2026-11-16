import { z } from 'zod';
import { fechaObligatoria, validarEstancia } from './estancia';
import { esquemaOcupante } from './ocupante';

/**
 * Reserva del huésped por el PORTAL. El primer ocupante es el titular (la persona de la sesión). Zod ayuda, pero no
 * decide: el backend valida el negocio (estancia mínima, adulto responsable, disponibilidad, capacidad...) con 409.
 */
export const esquemaReservaHuesped = z
  .object({
    fechaEntrada: fechaObligatoria('Elija la fecha de entrada'),
    fechaSalida: fechaObligatoria('Elija la fecha de salida'),
    /** Opcional al crear; es obligatoria para confirmar (RN-09). */
    horaEstimadaLlegada: z.string().regex(/^(\d{2}:\d{2})?$/, 'Hora no válida'),
    ocupantes: z.array(esquemaOcupante).min(1, 'La reserva debe tener al menos un ocupante'),
  })
  .superRefine((datos, contexto) => validarEstancia(datos, contexto));

export type FormularioReservaHuesped = z.infer<typeof esquemaReservaHuesped>;

/** Años cumplidos en la fecha dada (la edad se mide a la fecha de ENTRADA, no a hoy: L-09 y RP-03). */
export function edadALaFecha(fechaNacimiento: string, fecha: string): number {
  const [anioN = 0, mesN = 0, diaN = 0] = fechaNacimiento.split('-').map(Number);
  const [anio = 0, mes = 0, dia = 0] = fecha.split('-').map(Number);
  const yaCumplio = mes > mesN || (mes === mesN && dia >= diaN);
  return anio - anioN - (yaCumplio ? 0 : 1);
}
