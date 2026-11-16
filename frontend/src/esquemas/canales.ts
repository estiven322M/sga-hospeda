import { z } from 'zod';
import { TIPOS_CANAL } from '../modelos/canal';

/**
 * Esquema del formulario de un canal de venta. Solo valida la FORMA (el código de 2 a 12 caracteres, el nombre); que el código
 * ya exista lo responde el backend con 409 y se muestra tal cual.
 */
export const esquemaCanal = z.object({
  codigo: z
    .string()
    .trim()
    .regex(/^[A-Za-z0-9][A-Za-z0-9_-]{1,11}$/, 'El código lleva de 2 a 12 letras, números, guion o guion bajo, y empieza por letra o número'),
  nombre: z.string().trim().min(1, 'Ingrese el nombre del canal').max(80, 'El nombre no puede superar 80 caracteres'),
  tipo: z.enum(TIPOS_CANAL, 'Elija el tipo de canal'),
});
export type FormularioCanal = z.infer<typeof esquemaCanal>;
