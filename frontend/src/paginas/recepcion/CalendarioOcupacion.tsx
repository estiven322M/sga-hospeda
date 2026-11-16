import { useMemo, useState } from 'react';
import { Link } from 'react-router';
import { Campo } from '../../componentes/Campo';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { GrillaCalendario } from '../../componentes/GrillaCalendario';
import { useApartamentos } from '../../hooks/useApartamentos';
import { useConsulta } from '../../hooks/useConsulta';
import type { NocheCalendarioResponse } from '../../modelos/calendario';
import { RUTAS } from '../../rutas/rutas';
import { obtenerCalendario } from '../../servicios/calendarioServicio';
import { hoyISO, primerDiaDeMes } from '../../utilidades/fechas';
import { formatearRango } from '../../utilidades/formato';

const FECHA = /^\d{4}-\d{2}-\d{2}$/;

/** Una reserva del calendario con la primera y la última noche que ocupa dentro del rango consultado. */
interface ReservaEnElRango {
  codigo: string;
  estado: string;
  primera: string;
  ultima: string;
}

function reservasDe(noches: NocheCalendarioResponse[]): ReservaEnElRango[] {
  const porCodigo = new Map<string, ReservaEnElRango>();
  for (const noche of noches) {
    if (!noche.codigoReserva) continue;
    const existente = porCodigo.get(noche.codigoReserva);
    if (existente) existente.ultima = noche.noche;
    else porCodigo.set(noche.codigoReserva, { codigo: noche.codigoReserva, estado: noche.estadoReserva ?? '', primera: noche.noche, ultima: noche.noche });
  }
  return [...porCodigo.values()];
}

/**
 * Calendario de ocupación de recepción (7.1, 7.5): se elige un apartamento y un rango de fechas y se ve cada noche con su
 * color (libre, reservada según el estado de la reserva, o bloqueada). Las noches reservadas llevan a la gestión de su
 * reserva. El máximo de noches lo decide el backend (409 si se excede) y su mensaje se muestra tal cual.
 */
export default function CalendarioOcupacion() {
  const { apartamentos, error: errorApartamentos } = useApartamentos();
  const [apartamento, setApartamento] = useState('');
  const [desde, setDesde] = useState(hoyISO());
  const [hasta, setHasta] = useState(primerDiaDeMes(hoyISO(), 1));

  const errorDesde = FECHA.test(desde) ? undefined : 'Elija la fecha de inicio';
  const errorHasta = useMemo(() => {
    if (!FECHA.test(hasta)) return 'Elija la fecha de fin';
    if (FECHA.test(desde) && hasta <= desde) return 'La fecha de fin debe ser posterior a la de inicio';
    return undefined;
  }, [desde, hasta]);

  const clave = apartamento !== '' && !errorDesde && !errorHasta ? `${apartamento}|${desde}|${hasta}` : null;
  const { resultado, cargando } = useConsulta(clave, (signal) => obtenerCalendario(apartamento, desde, hasta, signal));
  const reservas = resultado?.tipo === 'datos' ? reservasDe(resultado.datos.noches) : [];

  return (
    <section>
      <h1 className="h3 mb-3">Calendario de ocupación</h1>

      {errorApartamentos && <div className="alert alert-danger" role="alert">{errorApartamentos}</div>}
      <form noValidate onSubmit={(evento) => evento.preventDefault()} aria-label="Consulta del calendario" className="row">
        <div className="col-12 col-md-5">
          <CampoSelect id="calendario-apartamento" etiqueta="Apartamento" value={apartamento} onChange={(evento) => setApartamento(evento.target.value)}>
            <option value="">Elija un apartamento</option>
            {apartamentos?.map((opcion) => (
              <option key={opcion.identificacion} value={opcion.identificacion}>
                {opcion.identificacion} · {opcion.nombre}
              </option>
            ))}
          </CampoSelect>
        </div>
        <div className="col-6 col-md-3">
          <Campo id="calendario-desde" type="date" etiqueta="Desde" value={desde} onChange={(evento) => setDesde(evento.target.value)} error={errorDesde} />
        </div>
        <div className="col-6 col-md-3">
          <Campo
            id="calendario-hasta"
            type="date"
            etiqueta="Hasta (la noche de esta fecha no se incluye)"
            value={hasta}
            onChange={(evento) => setHasta(evento.target.value)}
            error={errorHasta}
          />
        </div>
      </form>

      <div aria-live="polite">
        {apartamento === '' && <div className="alert alert-info">Elija un apartamento para ver su calendario.</div>}
        {cargando && <Cargando />}
        {/* 409: más noches de las que admite el calendario */}
        {resultado?.tipo === 'error' && <div className="alert alert-warning" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' && (
          <>
            <GrillaCalendario noches={resultado.datos.noches} conEnlaces />
            <h2 className="h6 mt-4">Reservas en este rango</h2>
            {reservas.length === 0 ? (
              <p className="text-body-secondary small">No hay reservas activas en este rango.</p>
            ) : (
              <ul className="small">
                {reservas.map((reserva) => (
                  <li key={reserva.codigo}>
                    <Link to={RUTAS.gestionReserva(reserva.codigo)}>{reserva.codigo}</Link> · {reserva.estado} · noches del{' '}
                    {formatearRango(reserva.primera, reserva.ultima).replace(' → ', ' al ')}
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </section>
  );
}
