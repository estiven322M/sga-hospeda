import type { ReservaDetalleResponse } from '../modelos/reserva';
import { formatearFecha, formatearFechaHora, formatearHora, formatearNoches, formatearPesos } from '../utilidades/formato';
import { EstadoReservaBadge } from './EstadoReservaBadge';
import { HistorialReserva } from './HistorialReserva';

/**
 * Todo lo que el backend dice de una reserva: datos, valor, ocupantes y historial. Es solo presentación: el valor
 * total es el valor NETO congelado (bruto menos el descuento por estancia larga, RP-02); no se recalcula.
 */
export function DatosReserva({ reserva }: { reserva: ReservaDetalleResponse }) {
  return (
    <div>
      <div className="card mb-3">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between gap-2">
            <h2 className="h5 mb-0">
              {reserva.codigo} <EstadoReservaBadge estado={reserva.estado} />
            </h2>
            <span className="badge text-bg-light border align-self-start">{reserva.canalOrigen}</span>
          </div>
          <dl className="row small mt-3 mb-0">
            <dt className="col-sm-4">Apartamento</dt>
            <dd className="col-sm-8">{reserva.identificacionApartamento}</dd>
            <dt className="col-sm-4">Estancia</dt>
            <dd className="col-sm-8">
              {formatearFecha(reserva.fechaEntrada)} → {formatearFecha(reserva.fechaSalida)} ({formatearNoches(reserva.noches)})
            </dd>
            <dt className="col-sm-4">Hora estimada de llegada</dt>
            <dd className="col-sm-8">{reserva.horaEstimadaLlegada ? formatearHora(reserva.horaEstimadaLlegada) : 'Sin informar'}</dd>
            {reserva.identificadorExterno && (
              <>
                <dt className="col-sm-4">Identificador externo</dt>
                <dd className="col-sm-8">{reserva.identificadorExterno}</dd>
              </>
            )}
            <dt className="col-sm-4">Titular</dt>
            <dd className="col-sm-8">
              {reserva.titular.nombre} ({reserva.titular.documento})
            </dd>
            <dt className="col-sm-4">Política de cancelación</dt>
            <dd className="col-sm-8">Versión {reserva.versionPolitica}, congelada al crear la reserva</dd>
            <dt className="col-sm-4">Creada</dt>
            <dd className="col-sm-8">{formatearFechaHora(reserva.creadaEn)}</dd>
            {reserva.porcentajeDescuento > 0 && (
              <>
                <dt className="col-sm-4">Subtotal</dt>
                <dd className="col-sm-8">{formatearPesos(reserva.subtotalBruto)}</dd>
                <dt className="col-sm-4 text-success">Descuento por estancia larga ({reserva.porcentajeDescuento} %)</dt>
                <dd className="col-sm-8 text-success">−{formatearPesos(reserva.descuento)}</dd>
              </>
            )}
            <dt className="col-sm-4">Valor total</dt>
            <dd className="col-sm-8 fw-semibold">{formatearPesos(reserva.valorTotal)}</dd>
          </dl>
        </div>
      </div>

      <h3 className="h6">Ocupantes</h3>
      <ul className="small">
        {reserva.ocupantes.map((ocupante) => (
          <li key={ocupante.documento}>
            {ocupante.nombre} ({ocupante.documento}), nació el {formatearFecha(ocupante.fechaNacimiento)}
            {ocupante.documento === reserva.titular.documento && ' · titular'}
          </li>
        ))}
      </ul>

      <h3 className="h6">Historial</h3>
      <HistorialReserva historial={reserva.historial} />
    </div>
  );
}
