import type { Caracteristica } from '../modelos/apartamento';

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
