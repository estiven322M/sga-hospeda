import { z } from 'zod';
import { hoyISO } from '../utilidades/fechas';
import { contrasenaSegura } from './contrasena';

/**
 * Esquemas de los formularios de usuarios, perfil y recuperación. Solo validan la FORMA (campos obligatorios, formatos, rangos
 * evidentes); lo que decide el negocio (correo o documento repetidos, contraseña actual incorrecta, desactivar a un titular con
 * reservas activas) lo responde el backend con 409 y se muestra tal cual. Los nombres de campo son los de los DTO.
 */

const telefono = z.string().trim().regex(/^\+?\d{7,15}$/, 'El teléfono solo admite dígitos (de 7 a 15) y un + inicial');
const nombre = z.string().trim().min(1, 'Ingrese el nombre').max(100, 'El nombre no puede superar 100 caracteres');
const correo = z.email('Ingrese un correo electrónico válido').max(120, 'El correo no puede superar 120 caracteres');

/** Crear un usuario interno (administrador). */
export const esquemaUsuarioInterno = z.object({
  nombre,
  correo,
  contrasena: contrasenaSegura,
  telefono,
  documento: z.string().trim().min(1, 'Ingrese el documento').max(15, 'El documento no puede superar 15 caracteres'),
  fechaNacimiento: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingrese la fecha de nacimiento')
    .refine((fecha) => fecha <= hoyISO(), 'La fecha de nacimiento no puede ser futura'),
  rol: z.enum(['RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO'], 'Elija el rol'),
});
export type FormularioUsuarioInterno = z.infer<typeof esquemaUsuarioInterno>;

/** Perfil propio: solo nombre y teléfono. */
export const esquemaPerfil = z.object({ nombre, telefono });
export type FormularioPerfil = z.infer<typeof esquemaPerfil>;

/** Cambio voluntario de contraseña: la actual y la nueva (con su confirmación, que no se envía). */
export const esquemaCambioContrasena = z
  .object({
    contrasenaActual: z.string().min(1, 'Ingrese su contraseña actual'),
    contrasenaNueva: contrasenaSegura,
    confirmarContrasena: z.string().min(1, 'Confirme la contraseña nueva'),
  })
  .refine((datos) => datos.contrasenaNueva === datos.confirmarContrasena, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmarContrasena'],
  });
export type FormularioCambioContrasena = z.infer<typeof esquemaCambioContrasena>;

/** Recuperación, paso 1: el correo al que se envía el código. */
export const esquemaSolicitarRecuperacion = z.object({ correo: z.email('Ingrese un correo electrónico válido') });
export type FormularioSolicitarRecuperacion = z.infer<typeof esquemaSolicitarRecuperacion>;

/** Recuperación, paso 2: el correo, el código de 6 dígitos y la contraseña nueva (con su confirmación). */
export const esquemaConfirmarRecuperacion = z
  .object({
    correo: z.email('Ingrese un correo electrónico válido'),
    codigo: z.string().trim().regex(/^\d{6}$/, 'El código tiene 6 dígitos'),
    contrasenaNueva: contrasenaSegura,
    confirmarContrasena: z.string().min(1, 'Confirme la contraseña nueva'),
  })
  .refine((datos) => datos.contrasenaNueva === datos.confirmarContrasena, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmarContrasena'],
  });
export type FormularioConfirmarRecuperacion = z.infer<typeof esquemaConfirmarRecuperacion>;
