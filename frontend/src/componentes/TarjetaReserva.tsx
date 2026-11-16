import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { ReservaResumenResponse } from '../modelos/reserva';
import { formatearPesos, formatearRango } from '../utilidades/formato';
import { EstadoReservaBadge } from './EstadoReservaBadge';

interface TarjetaReservaProps {
  reserva: ReservaResumenResponse;
  /** A dónde lleva el código de la reserva (su detalle o su gestión). */
  enlace: string;
  /** Acciones de la reserva (botones). Si no se pasan, la tarjeta solo informa. */
  children?: ReactNode;
}

/** Un renglón de un listado de reservas: estado, fechas, apartamento, titular y valor neto. No consulta nada. */
export function TarjetaReserva({ reserva, enlace, children }: TarjetaReservaProps) {
  return (
    <article className="card mb-3">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h2 className="h6 mb-1">
              <Link to={enlace}>{reserva.codigo}</Link>
            </h2>
            <EstadoReservaBadge estado={reserva.estado} />{' '}
            <span className="badge text-bg-light border">{reserva.canalOrigen}</span>
          </div>
          <div className="text-md-end">
            <div className="small text-body-secondary">Valor total</div>
            <div className="fs-5 fw-semibold">{formatearPesos(reserva.valorTotal)}</div>
          </div>
        </div>
        <ul className="list-unstyled small text-body-secondary mt-2 mb-0">
          <li>Estancia: {formatearRango(reserva.fechaEntrada, reserva.fechaSalida)}</li>
          <li>Apartamento: {reserva.identificacionApartamento}</li>
          <li>
            Titular: {reserva.nombreTitular} · {reserva.totalOcupantes} {reserva.totalOcupantes === 1 ? 'ocupante' : 'ocupantes'}
          </li>
        </ul>
        {children && <div className="mt-3">{children}</div>}
      </div>
    </article>
  );
}
