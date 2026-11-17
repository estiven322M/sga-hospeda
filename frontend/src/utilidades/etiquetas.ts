import type { Caracteristica } from '../modelos/apartamento';
import type { GravedadNovedad } from '../modelos/novedad';

/**
 * Texto para mostrar de cada característica del catálogo cerrado (L-06). El backend manda el código
 * (VISTA_AL_PARQUE); aquí solo se traduce a un texto legible. Un código nuevo del catálogo que aún no esté en
 * este mapa se muestra con sus guiones bajos convertidos en espacios.
 */
const ETIQUETAS: Record<Caracteristica, string> = {
  TERRAZA: 'Terraza',
  BALCON: 'Balcón',
  VISTA_AL_PARQUE: 'Vista al parque',
  VISTA_PANORAMICA: 'Vista panorámica',
  JACUZZI: 'Jacuzzi',
  PARQUEADERO: 'Parqueadero',
  ACCESIBLE: 'Accesible',
};

export function etiquetarCaracteristica(caracteristica: string): string {
  return (ETIQUETAS as Record<string, string>)[caracteristica] ?? caracteristica.replace(/_/g, ' ').toLowerCase();
}

/** Texto para mostrar de cada gravedad de una novedad (7.6). */
export const ETIQUETA_GRAVEDAD: Record<GravedadNovedad, string> = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};
