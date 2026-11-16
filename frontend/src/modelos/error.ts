import type { FechaHoraISO } from './comun';

/**
 * Espejo de ErrorResponse (openapi.yaml): la forma ÚNICA de todos los errores de la API.
 * - 400: forma (campos faltantes, JSON mal formado). Trae `errores` por campo.
 * - 401: sin token, token inválido o vencido, o credenciales erradas.
 * - 403: el rol no lo permite o no se es el titular de la reserva.
 * - 404: no existe.
 * - 409: una regla de negocio lo impide; `mensaje` explica cuál. Se muestra tal cual al usuario.
 */
export interface ErrorResponse {
  timestamp: FechaHoraISO;
  status: number;
  error: string;
  /** Mensaje con significado de negocio, en español. */
  mensaje: string;
  path: string;
  /** Solo en un 400 de validación (campo → mensaje); null en los demás errores. */
  errores?: Record<string, string> | null;
}
