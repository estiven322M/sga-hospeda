import { ROLES, type RolUsuario } from '../modelos/auth';

/** Lo que el frontend lee de un JWT del SGA: correo (`sub`), roles, documento y expiración (`exp`). */
export interface DatosToken {
  correo: string | null;
  documento: string | null;
  roles: RolUsuario[];
  /** Momento de expiración en milisegundos desde 1970 (`exp` del token está en segundos), o null si no lo trae. */
  expiraEnMs: number | null;
}

interface PayloadJwt {
  sub?: unknown;
  documento?: unknown;
  roles?: unknown;
  exp?: unknown;
}

/**
 * Lee los datos del payload de un JWT.
 * Decodificar NO es validar: cualquiera puede leer un JWT (se firma, no se cifra). Quien comprueba la firma y la
 * expiración es el backend; el frontend solo lo usa para MOSTRAR y para decidir qué menú pintar. Devuelve null si
 * el texto no es un JWT legible.
 */
export function leerToken(token: string): DatosToken | null {
  try {
    const payloadCodificado = token.split('.')[1];
    if (!payloadCodificado) return null;
    // Base64URL → Base64 (con relleno), y los bytes se decodifican como UTF-8 (tildes y eñes)
    const base64 = payloadCodificado.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payloadCodificado.length / 4) * 4, '=');
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
    const payload = JSON.parse(new TextDecoder().decode(bytes)) as PayloadJwt;

    const roles = Array.isArray(payload.roles)
      ? payload.roles.filter((rol): rol is RolUsuario => ROLES.includes(rol as RolUsuario))
      : [];
    return {
      correo: typeof payload.sub === 'string' ? payload.sub : null,
      documento: typeof payload.documento === 'string' ? payload.documento : null,
      roles,
      expiraEnMs: typeof payload.exp === 'number' ? payload.exp * 1000 : null,
    };
  } catch {
    return null;
  }
}

/** ¿El token ya expiró? Un token ilegible o sin `exp` se considera vencido: no sirve para nada. */
export function tokenVencido(token: string, ahoraMs: number = Date.now()): boolean {
  const datos = leerToken(token);
  return datos?.expiraEnMs == null || datos.expiraEnMs <= ahoraMs;
}
