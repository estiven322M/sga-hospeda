import type { FolioResponse } from '../modelos/folio';
import { formatearFecha, formatearPesos } from '../utilidades/formato';

/**
 * El folio tal como lo entrega el backend: cargos, ajustes, pagos y saldo. El saldo es cargos + ajustes − pagos; uno
 * negativo es saldo a favor del huésped (RN-15). No se calcula nada aquí.
 */
export function FolioDetalle({ folio }: { folio: FolioResponse }) {
  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
        <h3 className="h6 mb-0">Folio</h3>
        <span className={`badge ${folio.cerrado ? 'text-bg-secondary' : 'text-bg-success'}`}>{folio.cerrado ? 'Cerrado' : 'Abierto'}</span>
      </div>
      <div className="table-responsive">
        <table className="table table-sm align-middle">
          <thead>
            <tr>
              <th>Concepto</th>
              <th className="text-end">Valor</th>
            </tr>
          </thead>
          <tbody>
            {folio.cargos.map((cargo, indice) => (
              <tr key={`cargo-${indice}`}>
                <td>{cargo.concepto}</td>
                <td className="text-end">{formatearPesos(cargo.valor)}</td>
              </tr>
            ))}
            {folio.ajustes.map((ajuste, indice) => (
              <tr key={`ajuste-${indice}`} className="table-light">
                <td>Ajuste: {ajuste.concepto}</td>
                <td className="text-end">{formatearPesos(ajuste.valor)}</td>
              </tr>
            ))}
            {folio.pagos.map((pago, indice) => (
              <tr key={`pago-${indice}`} className="text-success">
                <td>
                  Pago ({pago.medio}, {formatearFecha(pago.fecha)})
                </td>
                <td className="text-end">−{formatearPesos(pago.valor)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th>Saldo</th>
              <th className="text-end">{formatearPesos(folio.saldo)}</th>
            </tr>
          </tfoot>
        </table>
      </div>
      {folio.autorizacionCierre && <p className="small text-body-secondary">Cierre autorizado: {folio.autorizacionCierre}</p>}
    </div>
  );
}
