import type { TokenResponse } from '../modelos/auth';
import { tokenVencido } from '../utilidades/jwt';

/**
 * Persistencia de la sesión en localStorage, para que sobreviva a recargar la página. Se guarda SOLO la respuesta
 * del login (token, tipo, expiración y roles): ni la contraseña ni ningún dato personal. Una sola clave.
 *
 * Un token en localStorage lo puede leer cualquier script de la página (XSS); por eso la aplicación no inserta HTML
 * que venga del servidor y el token dura 30 minutos.
 */
const CLAVE_SESION = 'sga.sesion';

export function guardarSesion(sesion: TokenResponse): void {
  localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
}

/** La sesión guardada tal cual, o null si no hay o está dañada. No revisa la expiración: para eso, sesionVigente. */
export function leerSesion(): TokenResponse | null {
  const texto = localStorage.getItem(CLAVE_SESION);
  if (!texto) return null;
  try {
    const sesion = JSON.parse(texto) as Partial<TokenResponse>;
    return typeof sesion.token === 'string' && Array.isArray(sesion.roles) ? (sesion as TokenResponse) : null;
  } catch {
    return null; // texto que no es JSON: se trata como si no hubiera sesión
  }
}

/** La sesión guardada solo si su token no ha expirado. Una sesión vencida o dañada se descarta. */
export function sesionVigente(): TokenResponse | null {
  const sesion = leerSesion();
  if (sesion && !tokenVencido(sesion.token)) {
    return sesion;
  }
  borrarSesion();
  return null;
}

export function obtenerToken(): string | null {
  return leerSesion()?.token ?? null;
}

export function borrarSesion(): void {
  localStorage.removeItem(CLAVE_SESION);
}
