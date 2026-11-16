import { z } from 'zod';

// Las reglas del FORMULARIO: forma del mensaje, no reglas del negocio. Los nombres de campo son EXACTAMENTE
// los de LoginRequest, así el formulario se envía tal cual al backend.
export const esquemaLogin = z.object({
  correo: z.email('Ingrese un correo electrónico válido'),
  contrasena: z.string().min(1, 'Ingrese su contraseña'),
});

// El tipo del formulario se DEDUCE del esquema: una sola fuente de verdad.
export type FormularioLogin = z.infer<typeof esquemaLogin>;
