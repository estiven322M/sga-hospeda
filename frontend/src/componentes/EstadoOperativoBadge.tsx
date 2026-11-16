import type { EstadoOperativo } from '../modelos/apartamento';

const COLOR_ESTADO: Record<EstadoOperativo, string> = {
  PREPARADO: 'text-bg-success',
  OCUPADO: 'text-bg-primary',
  PENDIENTE_PREPARACION: 'text-bg-warning',
  EN_PREPARACION: 'text-bg-info',
  FUERA_DE_SERVICIO: 'text-bg-danger',
};

const ETIQUETA_ESTADO: Record<EstadoOperativo, string> = {
  PREPARADO: 'Preparado',
  OCUPADO: 'Ocupado',
  PENDIENTE_PREPARACION: 'Pendiente de preparación',
  EN_PREPARACION: 'En preparación',
  FUERA_DE_SERVICIO: 'Fuera de servicio',
};

interface EstadoOperativoBadgeProps {
  estado: EstadoOperativo;
}

/** Condición física del apartamento (3.3). Componente de presentación, sin estado propio. */
export function EstadoOperativoBadge({ estado }: EstadoOperativoBadgeProps) {
  return <span className={`badge ${COLOR_ESTADO[estado]}`}>{ETIQUETA_ESTADO[estado]}</span>;
}
