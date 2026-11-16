import { z } from 'zod';
import { fechaObligatoria, validarEstancia } from './estancia';
import { esquemaOcupante } from './ocupante';

/**
 * Lo que comparten los formularios de reserva. El primer ocupante es el titular. Zod ayuda, pero no decide: el
 * backend valida el negocio (estancia mínima, adulto responsable, disponibilidad, capacidad...) con 409.
 */
const baseReserva = z.object({
  fechaEntrada: fechaObligatoria('Elija la fecha de entrada'),
  fechaSalida: fechaObligatoria('Elija la fecha de salida'),
  /** Opcional al crear; es obligatoria para confirmar (RN-09). */
  horaEstimadaLlegada: z.string().regex(/^(\d{2}:\d{2})?$/, 'Hora no válida'),
  ocupantes: z.array(esquemaOcupante).min(1, 'La reserva debe tener al menos un ocupante'),
});

/** Reserva del huésped por el PORTAL: el apartamento viene de la ruta. */
export const esquemaReservaHuesped = baseReserva.superRefine((datos, contexto) => validarEstancia(datos, contexto));
export type FormularioReservaHuesped = z.infer<typeof esquemaReservaHuesped>;

/** Reserva de recepción por DIRECTO: además se elige el apartamento. */
export const esquemaReservaRecepcion = baseReserva
  .extend({ identificacionApartamento: z.string().min(1, 'Elija el apartamento') })
  .superRefine((datos, contexto) => validarEstancia(datos, contexto));
export type FormularioReservaRecepcion = z.infer<typeof esquemaReservaRecepcion>;

/**
 * Modificar una reserva (CU-04): fechas, ocupantes y apartamento. La hora de llegada no cambia aquí y la entrada no se
 * compara con hoy (una reserva ya empezada o con fecha cercana la valida el backend).
 */
export const esquemaModificarReserva = z
  .object({
    identificacionApartamento: z.string().min(1, 'Elija el apartamento'),
    fechaEntrada: fechaObligatoria('Elija la fecha de entrada'),
    fechaSalida: fechaObligatoria('Elija la fecha de salida'),
    ocupantes: z.array(esquemaOcupante).min(1, 'La reserva debe tener al menos un ocupante'),
  })
  .superRefine((datos, contexto) => {
    if (/^\d{4}-\d{2}-\d{2}$/.test(datos.fechaEntrada) && /^\d{4}-\d{2}-\d{2}$/.test(datos.fechaSalida) && datos.fechaSalida <= datos.fechaEntrada) {
      contexto.addIssue({ code: 'custom', path: ['fechaSalida'], message: 'La salida debe ser posterior a la entrada' });
    }
  });
export type FormularioModificarReserva = z.infer<typeof esquemaModificarReserva>;

/** Registrar un pago (CU-08): medio, valor entero positivo en pesos y fecha. */
export const esquemaPago = z.object({
  medio: z.string().trim().min(1, 'Indique el medio de pago').max(40, 'El medio no puede superar 40 caracteres'),
  valor: z.string().regex(/^[1-9]\d*$/, 'El valor debe ser un número entero de pesos mayor que cero'),
  fecha: fechaObligatoria('Elija la fecha del pago'),
});
export type FormularioPago = z.infer<typeof esquemaPago>;

/** Cerrar un folio con saldo pendiente (CU-11): quién autoriza y por qué. */
export const esquemaAutorizacionCierre = z.object({
  autorizadoPor: z.string().trim().min(1, 'Indique quién autoriza el cierre'),
  motivo: z.string().trim().min(1, 'Indique el motivo del cierre con saldo'),
});
export type FormularioAutorizacionCierre = z.infer<typeof esquemaAutorizacionCierre>;

/** Años cumplidos en la fecha dada (la edad se mide a la fecha de ENTRADA, no a hoy: L-09 y RP-03). */
export function edadALaFecha(fechaNacimiento: string, fecha: string): number {
  const [anioN = 0, mesN = 0, diaN = 0] = fechaNacimiento.split('-').map(Number);
  const [anio = 0, mes = 0, dia = 0] = fecha.split('-').map(Number);
  const yaCumplio = mes > mesN || (mes === mesN && dia >= diaN);
  return anio - anioN - (yaCumplio ? 0 : 1);
}
