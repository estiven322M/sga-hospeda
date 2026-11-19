import type { PeriodoResponse } from '../modelos/tarifario';
import { formatearRango } from '../utilidades/formato';

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'] as const;

/** "2026-06-15" → "15 jun". Se lee del texto AAAA-MM-DD, sin Date, para no correr el día por la zona horaria. */
function diaYMes(fecha: string): string {
  const [, mes, dia] = fecha.split('-');
  return `${Number(dia)} ${MESES[Number(mes) - 1]}`;
}

/**
 * Los periodos de una temporada agrupados por el año en que empiezan, como etiquetas cortas ("15 jun → 15 jul"). Solo
 * presenta lo que llegó del backend: no ordena ni calcula nada. Al pasar el cursor se ve el rango completo con el año.
 */
export function PeriodosTemporada({ periodos, textoSinPeriodos }: { periodos: readonly PeriodoResponse[]; textoSinPeriodos: string }) {
  if (periodos.length === 0) {
    return <div className="small text-body-secondary">{textoSinPeriodos}</div>;
  }

  const porAnio = new Map<string, PeriodoResponse[]>();
  for (const periodo of periodos) {
    const anio = periodo.desde.slice(0, 4);
    porAnio.set(anio, [...(porAnio.get(anio) ?? []), periodo]);
  }

  return (
    <div className="periodos-temporada">
      {[...porAnio].map(([anio, delAnio]) => (
        <div key={anio} className="periodos-temporada__anio">
          <span className="periodos-temporada__etiqueta-anio">{anio}</span>
          <div className="periodos-temporada__lista">
            {delAnio.map((periodo) => (
              <span
                key={`${periodo.desde}-${periodo.hasta}`}
                className="periodos-temporada__periodo"
                title={formatearRango(periodo.desde, periodo.hasta)}
              >
                {diaYMes(periodo.desde)} → {diaYMes(periodo.hasta)}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
