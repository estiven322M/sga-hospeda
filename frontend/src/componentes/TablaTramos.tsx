import type { PoliticaCancelacionResponse } from '../modelos/tarifario';
import { formatearFecha } from '../utilidades/formato';

/**
 * Una versión de la política de cancelación: desde cuándo rige y sus tramos. Cada tramo dice cuánto se retiene (de lo
 * pagado por el alojamiento) según la antelación mínima en horas con que se cancela. Solo presentación.
 */
export function TablaTramos({ politica }: { politica: PoliticaCancelacionResponse }) {
  const tramos = [...politica.tramos].sort((a, b) => b.horasAntelacionMinima - a.horasAntelacionMinima);
  return (
    <div>
      <p className="small text-body-secondary mb-2">
        Versión {politica.version} · rige desde el {formatearFecha(politica.vigenteDesde)}
      </p>
      <div className="table-responsive">
        <table className="table table-sm align-middle mb-0">
          <thead>
            <tr>
              <th>Antelación mínima</th>
              <th className="text-end">Retención</th>
            </tr>
          </thead>
          <tbody>
            {tramos.map((tramo) => (
              <tr key={tramo.horasAntelacionMinima}>
                <td>{tramo.horasAntelacionMinima === 0 ? 'Cualquier otra antelación (0 horas)' : `${tramo.horasAntelacionMinima} horas o más`}</td>
                <td className="text-end">{tramo.porcentajeRetencion} %</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
