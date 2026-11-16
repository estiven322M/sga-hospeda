import { z } from 'zod';
import { hoyISO } from '../utilidades/fechas';

/** Un ocupante del grupo (OcupanteRequest). Solo la forma: la edad y quién paga lo decide el backend (L-09). */
export const esquemaOcupante = z.object({
  documento: z.string().trim().min(1, 'Ingrese el documento'),
  nombre: z.string().trim().min(1, 'Ingrese el nombre'),
  fechaNacimiento: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingrese la fecha de nacimiento')
    .refine((fecha) => fecha <= hoyISO(), 'La fecha de nacimiento no puede ser futura'),
});

export type FormularioOcupante = z.infer<typeof esquemaOcupante>;

export const OCUPANTE_VACIO: FormularioOcupante = { documento: '', nombre: '', fechaNacimiento: '' };
