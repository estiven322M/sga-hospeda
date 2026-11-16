import type { EventoReservaResponse } from '../modelos/reserva';
import { formatearFechaHora } from '../utilidades/formato';

/** Historial de eventos de una reserva, del más antiguo al más reciente tal como lo entrega el backend. */
export function HistorialReserva({ historial }: { historial: EventoReservaResponse[] }) {
  return (
    <div className="table-responsive">
      <table className="table table-sm align-middle">
        <thead>
          <tr>
            <th>Momento</th>
            <th>Acción</th>
            <th>Autor</th>
            <th>Estado</th>
            <th>Observación</th>
          </tr>
        </thead>
        <tbody>
          {historial.map((evento) => (
            <tr key={`${evento.momento}-${evento.accion}`}>
              <td>{formatearFechaHora(evento.momento)}</td>
              <td>{evento.accion}</td>
              <td>{evento.autor}</td>
              <td>
                {evento.estadoAnterior ? `${evento.estadoAnterior} → ` : ''}
                {evento.estadoNuevo}
              </td>
              <td>{evento.observacion}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
