import type { FechaISO } from './comun';
import type { RolUsuario } from './auth';

/** GET /usuarios y /usuarios/yo: un usuario del sistema. Nunca trae la contraseña ni su hash. `activo` es la eliminación lógica. */
export interface UsuarioResponse {
  id: number;
  nombre: string;
  correo: string;
  telefono: string;
  documento: string;
  fechaNacimiento: FechaISO;
  rol: RolUsuario;
  activo: boolean;
  /** URL de la foto de perfil (7.2); nula si no ha subido una: la pantalla muestra sus iniciales. */
  fotoUrl: string | null;
}

/** Roles de un usuario INTERNO: los únicos que un administrador asigna al crear (los huéspedes se registran solos). */
export type RolInterno = 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'PERSONAL_SERVICIO';

export const ROLES_INTERNOS: readonly RolInterno[] = ['RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO'];

/** POST /usuarios (ADMINISTRADOR). Correo y documento únicos (409). */
export interface CrearUsuarioInternoRequest {
  /** Máximo 100 caracteres. */
  nombre: string;
  /** Correo válido, máximo 120 caracteres. */
  correo: string;
  /** De 8 a 72 caracteres, con al menos una mayúscula y un número. */
  contrasena: string;
  /** Solo dígitos (de 7 a 15) y un + inicial opcional. */
  telefono: string;
  /** Máximo 15 caracteres. */
  documento: string;
  fechaNacimiento: FechaISO;
  rol: RolInterno;
}

/** PUT /usuarios/{id}/rol (ADMINISTRADOR). Un administrador no puede cambiar el suyo (409). */
export interface CambiarRolRequest {
  rol: RolUsuario;
}

/** PUT /usuarios/yo: solo nombre y teléfono; el correo, el documento y el rol no se editan. */
export interface ActualizarPerfilRequest {
  nombre: string;
  telefono: string;
}

/** PUT /usuarios/yo/contrasena. 409 si la actual no es correcta o la nueva es igual a la actual. */
export interface CambiarContrasenaRequest {
  contrasenaActual: string;
  /** De 8 a 72 caracteres, con al menos una mayúscula y un número (las reglas del registro). */
  contrasenaNueva: string;
}

/** POST /auth/recuperacion/solicitar (público). */
export interface SolicitarRecuperacionRequest {
  correo: string;
}

/** POST /auth/recuperacion/confirmar (público). */
export interface ConfirmarRecuperacionRequest {
  correo: string;
  /** Seis dígitos. */
  codigo: string;
  contrasenaNueva: string;
}

/** Respuesta de texto de las operaciones sin recurso que devolver. */
export interface MensajeResponse {
  mensaje: string;
}
