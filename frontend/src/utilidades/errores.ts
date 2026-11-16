import axios from 'axios';
import type { ErrorResponse } from '../modelos/error';

export const MENSAJE_SIN_CONEXION = 'No se pudo conectar con el servidor';

/**
 * Traduce cualquier error de una petición a un mensaje para el usuario.
 * - Sin respuesta (backend apagado, URL mal escrita o CORS bloqueado): "No se pudo conectar con el servidor".
 * - Con ErrorResponse del backend: se muestra su `mensaje` tal cual. En un 409 ES la regla de negocio explicada
 *   (por ejemplo "En temporada ALTA la estancia mínima es de 3 noches"); en un 401 del login, el mensaje genérico.
 * - Sin mensaje del backend: un texto genérico según el código.
 */
export function mensajeDeError(error: unknown): string {
  if (!axios.isAxiosError<ErrorResponse>(error)) {
    return 'Ocurrió un error inesperado';
  }
  if (!error.response) {
    return MENSAJE_SIN_CONEXION;
  }
  const { status, data } = error.response;
  if (typeof data?.mensaje === 'string' && data.mensaje) {
    return data.mensaje;
  }
  if (status === 403) return 'No tiene permiso para realizar esta acción';
  if (status === 404) return 'No se encontró lo que busca';
  return `El servidor respondió con un error inesperado (${status})`;
}

/** Código HTTP de la respuesta, o undefined si no hubo respuesta (error de red). */
export function estadoHttp(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined;
}

/**
 * Errores de campo de un 400 de Bean Validation, con el nombre que usa React Hook Form: el backend manda
 * "ocupantes[1].documento" y el formulario lo conoce como "ocupantes.1.documento". Vacío si el error no es un 400
 * con `errores`.
 */
export function erroresDeCampo(error: unknown): Record<string, string> {
  if (!axios.isAxiosError<ErrorResponse>(error) || !error.response?.data?.errores) {
    return {};
  }
  const resultado: Record<string, string> = {};
  for (const [campo, mensaje] of Object.entries(error.response.data.errores)) {
    resultado[campo.replace(/\[(\d+)\]/g, '.$1')] = mensaje;
  }
  return resultado;
}
