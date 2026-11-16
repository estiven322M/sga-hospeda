import { z } from 'zod';
import { CANALES_ORIGEN, ESTADOS_RESERVA } from '../modelos/reserva';

/** Fecha "AAAA-MM-DD" o vacía (filtro sin usar). */
const fechaOpcional = z.string().regex(/^(\d{4}-\d{2}-\d{2})?$/, 'Fecha no válida');

/**
 * Forma de los filtros del listado de reservas: los rangos deben tener el "desde" antes del "hasta". Es solo forma;
 * qué reservas salen lo decide el backend.
 */
export const esquemaFiltrosReservas = z
  .object({
    estado: z.union([z.literal(''), z.enum(ESTADOS_RESERVA)]),
    nombreTitular: z.string(),
    apartamento: z.string(),
    canal: z.union([z.literal(''), z.enum(CANALES_ORIGEN)]),
    entradaDesde: fechaOpcional,
    entradaHasta: fechaOpcional,
    salidaDesde: fechaOpcional,
    salidaHasta: fechaOpcional,
  })
  .superRefine((filtros, contexto) => {
    if (filtros.entradaDesde && filtros.entradaHasta && filtros.entradaDesde > filtros.entradaHasta) {
      contexto.addIssue({ code: 'custom', path: ['entradaHasta'], message: 'La fecha "hasta" no puede ser anterior a la fecha "desde"' });
    }
    if (filtros.salidaDesde && filtros.salidaHasta && filtros.salidaDesde > filtros.salidaHasta) {
      contexto.addIssue({ code: 'custom', path: ['salidaHasta'], message: 'La fecha "hasta" no puede ser anterior a la fecha "desde"' });
    }
  });
