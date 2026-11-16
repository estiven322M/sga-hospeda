import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { CalendarioDisponibilidad } from '../../componentes/CalendarioDisponibilidad';
import { Cargando } from '../../componentes/Cargando';
import { DesgloseCotizacion } from '../../componentes/DesgloseCotizacion';
import { EtiquetasCaracteristicas } from '../../componentes/EtiquetasCaracteristicas';
import { EstadoOperativoBadge } from '../../componentes/EstadoOperativoBadge';
import { InformacionAlojamiento } from '../../componentes/InformacionAlojamiento';
import { esquemaCotizacion } from '../../esquemas/estancia';
import { useAuth } from '../../hooks/useAuth';
import { useFiltros } from '../../hooks/useFiltros';
import type { ApartamentoResponse } from '../../modelos/apartamento';
import type { CotizacionResponse } from '../../modelos/cotizacion';
import { RUTAS } from '../../rutas/rutas';
import { obtenerApartamento } from '../../servicios/apartamentoServicio';
import { cotizarEstancia } from '../../servicios/cotizacionServicio';
import { estadoHttp, fueCancelada, mensajeDeError } from '../../utilidades/errores';

type Carga =
  | { tipo: 'listo'; apartamento: ApartamentoResponse }
  | { tipo: 'error'; mensaje: string; noExiste: boolean };

/** La carga guardada con el apartamento que se pidió: si ya es otro, se está cargando (se deriva, no se guarda). */
interface CargaDeApartamento {
  identificacion: string;
  carga: Carga;
}

type Cotizacion =
  | { tipo: 'ninguna' }
  | { tipo: 'cargando' }
  | { tipo: 'lista'; cotizacion: CotizacionResponse }
  | { tipo: 'error'; mensaje: string };

/**
 * Detalle de un apartamento y su cotización (CU-01). Cotiza con las fechas y los ocupantes que quedaron en los
 * filtros de la búsqueda. El desglose y el total los calcula el backend; aquí solo se muestran.
 */
export default function DetalleApartamento() {
  const { identificacion = '' } = useParams();
  const navigate = useNavigate();
  const { tieneRol } = useAuth();
  const { estado } = useFiltros();

  const [guardada, setGuardada] = useState<CargaDeApartamento | null>(null);
  useEffect(() => {
    const controlador = new AbortController();
    obtenerApartamento(identificacion, controlador.signal)
      .then((apartamento) => setGuardada({ identificacion, carga: { tipo: 'listo', apartamento } }))
      .catch((error: unknown) => {
        if (fueCancelada(error)) return;
        setGuardada({
          identificacion,
          carga: { tipo: 'error', mensaje: mensajeDeError(error), noExiste: estadoHttp(error) === 404 },
        });
      });
    return () => controlador.abort();
  }, [identificacion]);
  const carga = guardada?.identificacion === identificacion ? guardada.carga : null;

  // Para cotizar hacen falta fechas válidas y un grupo completo; los demás filtros (precio, características) no cuentan.
  const estancia = useMemo(() => esquemaCotizacion.safeParse(estado.filtros), [estado.filtros]);

  const [cotizacion, setCotizacion] = useState<Cotizacion>({ tipo: 'ninguna' });
  const controladorCotizacion = useRef<AbortController | null>(null);
  useEffect(() => () => controladorCotizacion.current?.abort(), []);

  function cotizar() {
    if (!estancia.success) return;
    controladorCotizacion.current?.abort();
    const controlador = new AbortController();
    controladorCotizacion.current = controlador;
    setCotizacion({ tipo: 'cargando' });
    cotizarEstancia(
      {
        identificacionApartamento: identificacion,
        fechaEntrada: estancia.data.fechaEntrada,
        fechaSalida: estancia.data.fechaSalida,
        ocupantes: estancia.data.ocupantes,
      },
      controlador.signal,
    )
      .then((lista) => setCotizacion({ tipo: 'lista', cotizacion: lista }))
      .catch((error: unknown) => {
        if (fueCancelada(error)) return;
        setCotizacion({ tipo: 'error', mensaje: mensajeDeError(error) });
      });
  }

  if (carga === null) return <Cargando />;
  if (carga.tipo === 'error') {
    return (
      <section>
        <h1 className="h3">Detalle del apartamento</h1>
        <div className="alert alert-danger" role="alert">
          {carga.noExiste ? `No existe el apartamento ${identificacion}.` : carga.mensaje}
        </div>
        <Link to={RUTAS.buscar} className="btn btn-outline-primary">Volver a la búsqueda</Link>
      </section>
    );
  }

  const { apartamento } = carga;
  return (
    <section>
      <h1 className="h3">Detalle del apartamento</h1>
      <div className="card mb-4">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
            <h2 className="h4 card-title mb-0">{apartamento.nombre}</h2>
            <EstadoOperativoBadge estado={apartamento.estadoOperativo} />
          </div>
          <p className="mt-2">{apartamento.descripcion}</p>
          <ul className="list-unstyled small text-body-secondary mb-2">
            <li>Identificación: {apartamento.identificacion}</li>
            <li>Dormitorios: {apartamento.dormitorios}</li>
            <li>Capacidad: {apartamento.capacidad} personas</li>
          </ul>
          {apartamento.dotacion.length > 0 && (
            <>
              <h3 className="h6">Dotación</h3>
              <ul className="mb-3">
                {apartamento.dotacion.map((elemento) => (
                  <li key={elemento}>{elemento}</li>
                ))}
              </ul>
            </>
          )}
          {apartamento.caracteristicas.length > 0 && (
            <>
              <h3 className="h6">Características</h3>
              <EtiquetasCaracteristicas caracteristicas={apartamento.caracteristicas} />
            </>
          )}
        </div>
      </div>

      <h2 className="h5">Disponibilidad</h2>
      <div className="mb-4">
        <CalendarioDisponibilidad identificacion={identificacion} />
      </div>

      <div className="mb-4">
        <InformacionAlojamiento />
      </div>

      <h2 className="h5">Cotización</h2>
      {!estancia.success ? (
        <div className="alert alert-info">
          Para cotizar, elija primero las fechas y los ocupantes en la <Link to={RUTAS.buscar}>búsqueda</Link>.
        </div>
      ) : (
        <>
          <button type="button" className="btn btn-primary mb-3" onClick={cotizar} disabled={cotizacion.tipo === 'cargando'}>
            {cotizacion.tipo === 'cargando' ? 'Cotizando…' : 'Cotizar'}
          </button>
          {cotizacion.tipo === 'error' && (
            // 409 (estancia mínima, RP-01): el mensaje del dominio tal cual
            <div className="alert alert-warning" role="alert">{cotizacion.mensaje}</div>
          )}
          {cotizacion.tipo === 'lista' && (
            <div className="card">
              <div className="card-body">
                <DesgloseCotizacion cotizacion={cotizacion.cotizacion} />
                {tieneRol('HUESPED') && (
                  <button
                    type="button"
                    className="btn btn-success"
                    onClick={() => void navigate(RUTAS.nuevaReservaHuesped(identificacion))}
                  >
                    Reservar
                  </button>
                )}
              </div>
            </div>
          )}
        </>
      )}
    </section>
  );
}
