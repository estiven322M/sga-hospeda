import { z } from 'zod';

/**
 * Contraseña segura (7.2): de 8 a 72 caracteres, con al menos una mayúscula y un número. Es la MISMA regla del registro, del
 * cambio de contraseña y de la recuperación (ReglasContrasena en el backend); aquí solo avisa de la forma antes de enviar.
 */
export const contrasenaSegura = z
  .string()
  .min(8, 'La contraseña debe tener al menos 8 caracteres')
  .max(72, 'La contraseña no puede superar 72 caracteres')
  .regex(/[A-Z]/, 'La contraseña debe tener al menos una letra mayúscula')
  .regex(/\d/, 'La contraseña debe tener al menos un número');
