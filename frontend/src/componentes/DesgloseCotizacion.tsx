import type { CotizacionResponse } from '../modelos/cotizacion';
import { formatearFecha, formatearNoches, formatearPesos } from '../utilidades/formato';

/**
 * Cotización noche por noche (RN-05). Muestra lo que el backend calculó, sin recalcular: el desglose es el valor
 * ANTES del descuento; si hay descuento por estancia larga (RP-02) se ven el subtotal bruto, el porcentaje y el valor
 * descontado, y el total ya con el descuento.
 */
export function DesgloseCotizacion({ cotizacion }: { cotizacion: CotizacionResponse }) {
  const conDescuento = cotizacion.porcentajeDescuento > 0;

  return (
    <div>
      <p className="text-body-secondary small">
        {formatearFecha(cotizacion.fechaEntrada)} → {formatearFecha(cotizacion.fechaSalida)} ({formatearNoches(cotizacion.noches)})
      </p>
      <div className="table-responsive">
        <table className="table table-sm align-middle">
          <thead>
            <tr>
              <th>Noche</th>
              <th>Temporada</th>
              <th className="text-end">Tarifa por ocupante</th>
              <th className="text-end">Ocupantes facturables</th>
              <th className="text-end">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {cotizacion.desglose.map((cargo) => (
              <tr key={cargo.noche}>
                <td>{formatearFecha(cargo.noche)}</td>
                <td>{cargo.temporada}</td>
                <td className="text-end">{formatearPesos(cargo.tarifaPorOcupante)}</td>
                <td className="text-end">{cargo.ocupantesFacturables}</td>
                <td className="text-end">{formatearPesos(cargo.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            {conDescuento && (
              <>
                <tr>
                  <th colSpan={4} className="text-end fw-normal">Subtotal</th>
                  <td className="text-end">{formatearPesos(cotizacion.subtotalBruto)}</td>
                </tr>
                <tr className="text-success">
                  <th colSpan={4} className="text-end fw-normal">
                    Descuento por estancia larga ({cotizacion.porcentajeDescuento} %)
                  </th>
                  <td className="text-end">−{formatearPesos(cotizacion.descuento)}</td>
                </tr>
              </>
            )}
            <tr>
              <th colSpan={4} className="text-end">Total</th>
              <th className="text-end">{formatearPesos(cotizacion.total)}</th>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}
