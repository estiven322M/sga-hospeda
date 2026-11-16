import { z } from 'zod';
import { hoyISO } from '../utilidades/fechas';

/**
 * Reglas de FORMA del registro: las mismas que el backend valida con Bean Validation (RegistroUsuarioRequest), para
 * avisar antes de enviar. Si el negocio lo rechaza (correo o documento ya registrados), responde 409 y se muestra
 * su mensaje. Los nombres de campo son EXACTAMENTE los de RegistroUsuarioRequest; `confirmarContrasena` solo existe
 * en el formulario y no se envía.
 */
export const esquemaRegistro = z
  .object({
    nombre: z.string().trim().min(1, 'Ingrese su nombre').max(100, 'El nombre no puede superar 100 caracteres'),
    correo: z.email('Ingrese un correo electrónico válido').max(120, 'El correo no puede superar 120 caracteres'),
    contrasena: z
      .string()
      .min(8, 'La contraseña debe tener al menos 8 caracteres')
      .max(72, 'La contraseña no puede superar 72 caracteres')
      .regex(/[A-Z]/, 'La contraseña debe tener al menos una letra mayúscula')
      .regex(/\d/, 'La contraseña debe tener al menos un número'),
    confirmarContrasena: z.string().min(1, 'Confirme su contraseña'),
    telefono: z
      .string()
      .trim()
      .regex(/^\+?\d{7,15}$/, 'El teléfono solo admite dígitos (de 7 a 15) y un + inicial'),
    documento: z.string().trim().min(1, 'Ingrese su documento').max(15, 'El documento no puede superar 15 caracteres'),
    fechaNacimiento: z
      .string()
      .regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingrese su fecha de nacimiento')
      .refine((fecha) => fecha <= hoyISO(), 'La fecha de nacimiento no puede ser futura'),
  })
  // refine sobre el objeto: compara DOS campos, algo que un campo solo no puede hacer
  .refine((datos) => datos.contrasena === datos.confirmarContrasena, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmarContrasena'],
  });

export type FormularioRegistro = z.infer<typeof esquemaRegistro>;
