import type { GravedadNovedad } from '../modelos/novedad';
import { ETIQUETA_GRAVEDAD } from '../utilidades/etiquetas';

const COLOR_GRAVEDAD: Record<GravedadNovedad, string> = {
  BAJA: 'text-bg-secondary',
  MEDIA: 'text-bg-info',
  ALTA: 'text-bg-warning',
  CRITICA: 'text-bg-danger',
};

/** Gravedad de una novedad (7.6). Componente de presentación, sin estado propio. */
export function GravedadBadge({ gravedad }: { gravedad: GravedadNovedad }) {
  return <span className={`badge ${COLOR_GRAVEDAD[gravedad]}`}>{ETIQUETA_GRAVEDAD[gravedad]}</span>;
}
