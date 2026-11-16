import { Link } from 'react-router';
import type { NocheCalendarioResponse } from '../modelos/calendario';
import { RUTAS } from '../rutas/rutas';

const DIAS_SEMANA = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
const NOMBRE_MES = new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric' });

/** Clases de Bootstrap de cada noche según su estado (y, si está reservada, según el estado de la reserva). */
function claseDeNoche(noche: NocheCalendarioResponse): string {
  switch (noche.estado) {
    case 'LIBRE':
      return 'bg-success-subtle text-success-emphasis';
    case 'BLOQUEADA':
      return 'bg-dark text-white';
    case 'NO_DISPONIBLE':
      return 'bg-secondary-subtle text-body-secondary';
    case 'RESERVADA':
      if (noche.estadoReserva === 'EN_CURSO') return 'bg-success text-white';
      if (noche.estadoReserva === 'CONFIRMADA') return 'bg-primary text-white';
      return 'bg-warning-subtle text-warning-emphasis'; // PENDIENTE
  }
}

function descripcionDeNoche(noche: NocheCalendarioResponse): string {
  switch (noche.estado) {
    case 'LIBRE':
      return `${noche.noche}: libre`;
    case 'BLOQUEADA':
      return `${noche.noche}: bloqueada`;
    case 'NO_DISPONIBLE':
      return `${noche.noche}: no disponible`;
    case 'RESERVADA':
      return `${noche.noche}: reservada${noche.codigoReserva ? ` (${noche.codigoReserva}, ${noche.estadoReserva ?? ''})` : ''}`;
  }
}

/** Agrupa las noches por mes ("AAAA-MM"), conservando el orden. */
function porMes(noches: NocheCalendarioResponse[]): [string, NocheCalendarioResponse[]][] {
  const meses = new Map<string, NocheCalendarioResponse[]>();
  for (const noche of noches) {
    const clave = noche.noche.slice(0, 7);
    meses.set(clave, [...(meses.get(clave) ?? []), noche]);
  }
  return [...meses.entries()];
}

interface GrillaCalendarioProps {
  noches: NocheCalendarioResponse[];
  /** Si es true, las noches reservadas llevan a la gestión de su reserva (solo recepción y administración la ven). */
  conEnlaces?: boolean;
}

/**
 * Calendario por noche, un mes por cuadrícula de 7 columnas (lunes a domingo). Cada noche tiene su color según su estado y
 * la leyenda lo explica. Solo presenta lo que el backend entregó: qué ve cada rol lo decide el backend.
 */
export function GrillaCalendario({ noches, conEnlaces = false }: GrillaCalendarioProps) {
  const estadosPresentes = new Set(noches.map((noche) => noche.estado));
  const leyenda: [string, string][] = [
    ['LIBRE', 'bg-success-subtle'],
    ['NO_DISPONIBLE', 'bg-secondary-subtle'],
    ['BLOQUEADA', 'bg-dark'],
    ['RESERVADA', 'bg-primary'],
  ];

  return (
    <div>
      <ul className="list-inline small mb-3">
        {leyenda
          .filter(([estado]) => estadosPresentes.has(estado as NocheCalendarioResponse['estado']))
          .map(([estado, clase]) => (
            <li key={estado} className="list-inline-item">
              <span className={`d-inline-block rounded border me-1 ${clase}`} style={{ width: 14, height: 14, verticalAlign: 'middle' }} />
              {estado === 'NO_DISPONIBLE' ? 'No disponible' : estado === 'LIBRE' ? 'Libre' : estado === 'BLOQUEADA' ? 'Bloqueada' : 'Reservada'}
            </li>
          ))}
      </ul>

      <div className="row g-3">
        {porMes(noches).map(([mes, delMes]) => {
          const primera = delMes[0];
          if (!primera) return null;
          const fechaPrimera = new Date(`${primera.noche}T00:00:00`);
          const huecosIniciales = (fechaPrimera.getDay() + 6) % 7; // lunes = 0
          return (
            <div key={mes} className="col-12 col-md-6 col-xl-4">
              <h3 className="h6 text-capitalize">{NOMBRE_MES.format(fechaPrimera)}</h3>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4 }}>
                {DIAS_SEMANA.map((dia) => (
                  <div key={dia} className="text-center small text-body-secondary">{dia}</div>
                ))}
                {Array.from({ length: huecosIniciales }, (_, i) => (
                  <div key={`hueco-${i}`} />
                ))}
                {delMes.map((noche) => {
                  const dia = Number(noche.noche.slice(8, 10));
                  const clases = `rounded border text-center small py-1 ${claseDeNoche(noche)}`;
                  return conEnlaces && noche.codigoReserva ? (
                    <Link
                      key={noche.noche}
                      to={RUTAS.gestionReserva(noche.codigoReserva)}
                      className={`${clases} text-decoration-none`}
                      title={descripcionDeNoche(noche)}
                      data-noche={noche.noche}
                    >
                      {dia}
                    </Link>
                  ) : (
                    <div key={noche.noche} className={clases} title={descripcionDeNoche(noche)} data-noche={noche.noche}>
                      {dia}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
