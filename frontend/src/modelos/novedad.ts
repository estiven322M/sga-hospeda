import type { FechaHoraISO } from './comun';

/** Gravedad de una novedad (7.6), de menor a mayor: los 4 valores del enum GravedadNovedad del backend. */
export const GRAVEDADES = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'] as const;

export type GravedadNovedad = (typeof GRAVEDADES)[number];

/** GET y POST /apartamentos/{identificacion}/novedades: una novedad del historial de un apartamento. */
export interface NovedadResponse {
  identificacionApartamento: string;
  /** La pone el servidor al registrar. */
  fecha: FechaHoraISO;
  /** Nombre de quien la registró: sale del token, no se envía. */
  autor: string;
  descripcion: string;
  gravedad: GravedadNovedad;
}

/** POST /apartamentos/{identificacion}/novedades. El autor y la fecha no viajan. */
export interface RegistrarNovedadRequest {
  /** Obligatoria, máximo 500 caracteres. */
  descripcion: string;
  gravedad: GravedadNovedad;
}
