import type { FechaHoraISO, FechaISO } from './comun';

/** Roles del sistema (RolUsuario). Viajan sin prefijo ROLE_. */
export type RolUsuario = 'HUESPED' | 'RECEPCIONISTA' | 'ADMINISTRADOR' | 'PERSONAL_SERVICIO';

/** Todos los roles, para menús y validaciones. */
export const ROLES: readonly RolUsuario[] = ['HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO'];

/** POST /auth/login */
export interface LoginRequest {
  correo: string;
  contrasena: string;
}

/** Respuesta del login. El token (JWT HS256) dura 30 minutos y trae `sub` (correo), `roles` y `documento`. */
export interface TokenResponse {
  token: string;
  /** "Bearer". */
  tipo: string;
  expiraEn: FechaHoraISO;
  roles: RolUsuario[];
}

/** POST /auth/registro (público): siempre crea un HUESPED. */
export interface RegistroUsuarioRequest {
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
}

export interface UsuarioRegistradoResponse {
  nombre: string;
  correo: string;
  documento: string;
  rol: RolUsuario;
}
