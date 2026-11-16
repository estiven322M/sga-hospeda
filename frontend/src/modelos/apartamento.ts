import type { FechaISO } from './comun';
import type { ReservaResumenResponse } from './reserva';

/** Condición física del apartamento (3.3). */
export type EstadoOperativo =
  | 'PREPARADO'
  | 'OCUPADO'
  | 'PENDIENTE_PREPARACION'
  | 'EN_PREPARACION'
  | 'FUERA_DE_SERVICIO';

/** Todos los estados operativos, en el orden del ciclo de preparación. */
export const ESTADOS_OPERATIVOS: readonly EstadoOperativo[] = [
  'PREPARADO',
  'OCUPADO',
  'PENDIENTE_PREPARACION',
  'EN_PREPARACION',
  'FUERA_DE_SERVICIO',
];

/**
 * Catálogo CERRADO de características de un apartamento (L-06): los 7 valores del enum del backend.
 * Esta constante solo sirve para tipar y validar; la lista que se MUESTRA (filtros, formularios) se pide
 * a GET /api/caracteristicas, para no duplicar el catálogo en el frontend.
 */
export const CARACTERISTICAS = [
  'TERRAZA',
  'BALCON',
  'VISTA_AL_PARQUE',
  'VISTA_PANORAMICA',
  'JACUZZI',
  'PARQUEADERO',
  'ACCESIBLE',
] as const;

export type Caracteristica = (typeof CARACTERISTICAS)[number];

/** GET /apartamentos y /apartamentos/{identificacion}. `activo` indica si se puede vender. */
export interface ApartamentoResponse {
  identificacion: string;
  nombre: string;
  descripcion: string;
  dormitorios: number;
  capacidad: number;
  dotacion: string[];
  caracteristicas: Caracteristica[];
  estadoOperativo: EstadoOperativo;
  activo: boolean;
}

/**
 * PUT /apartamentos/{identificacion}: los mismos campos de ApartamentoResponse más el aviso de capacidad (7.3). Si se bajó la
 * capacidad, `reservasPorEncimaDeLaCapacidad` lista las reservas activas con más ocupantes que la nueva capacidad; la
 * actualización no se bloquea y esas reservas no se modifican. Vacía si no se bajó la capacidad o no hay ninguna.
 */
export interface ApartamentoActualizadoResponse extends ApartamentoResponse {
  reservasPorEncimaDeLaCapacidad: ReservaResumenResponse[];
}

/** POST /apartamentos. Nace inactivo hasta tener tarifa en todas las temporadas (7.4). */
export interface CrearApartamentoRequest {
  identificacion: string;
  nombre: string;
  /** Obligatoria, máximo 500 caracteres. */
  descripcion: string;
  /** Al menos 1. */
  dormitorios: number;
  /** Al menos 1. */
  capacidad: number;
  /** Opcional, hasta 30 elementos de hasta 60 caracteres; sin repetidos (409 si se repite uno). */
  dotacion?: string[];
  /** Opcional; valores del catálogo cerrado. */
  caracteristicas?: Caracteristica[];
}

/** PUT /apartamentos/{identificacion}: reemplaza todos los datos editables. La identificación no cambia (L-04). */
export interface ActualizarApartamentoRequest {
  nombre: string;
  descripcion: string;
  dormitorios: number;
  capacidad: number;
  dotacion?: string[];
  caracteristicas?: Caracteristica[];
}

/** Bloqueo de un apartamento por rango de fechas (CU-09). */
export interface BloqueoResponse {
  identificacionApartamento: string;
  fechaInicio: FechaISO;
  fechaFin: FechaISO;
  motivo: string;
}

/** POST /apartamentos/{identificacion}/bloqueos. */
export interface RegistrarBloqueoRequest {
  fechaInicio: FechaISO;
  fechaFin: FechaISO;
  motivo: string;
}
