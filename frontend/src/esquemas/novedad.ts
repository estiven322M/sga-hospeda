import { z } from 'zod';
import { GRAVEDADES } from '../modelos/novedad';

/**
 * Formulario de una novedad (7.6). Solo valida la FORMA, con los mismos límites que el backend: descripción obligatoria de
 * máximo 500 caracteres y una gravedad del catálogo. La gravedad arranca sin elegir (la opción vacía del
 * <select> no pertenece al catálogo) para que quien registra la elija a conciencia.
 */
export const esquemaNovedad = z.object({
  descripcion: z.string().trim().min(1, 'Describa la novedad').max(500, 'La descripción no puede superar 500 caracteres'),
  gravedad: z.enum(GRAVEDADES, 'Elija la gravedad'),
});
export type FormularioNovedad = z.infer<typeof esquemaNovedad>;
