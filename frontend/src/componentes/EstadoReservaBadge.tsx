import type { EstadoReserva } from '../modelos/reserva';

const COLOR_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'text-bg-warning',
  CONFIRMADA: 'text-bg-primary',
  EN_CURSO: 'text-bg-success',
  FINALIZADA: 'text-bg-secondary',
  CANCELADA: 'text-bg-danger',
  NO_SHOW: 'text-bg-dark',
};

const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_SHOW: 'No-show',
};

interface EstadoReservaBadgeProps {
  estado: EstadoReserva;
}

/** Componente de presentación: recibe el estado y lo pinta. No consulta nada ni guarda estado. */
export function EstadoReservaBadge({ estado }: EstadoReservaBadgeProps) {
  return <span className={`badge ${COLOR_ESTADO[estado]}`}>{ETIQUETA_ESTADO[estado]}</span>;
}
