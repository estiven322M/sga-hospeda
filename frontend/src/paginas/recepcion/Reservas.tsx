import { useEffect, useMemo, useState, type ChangeEvent } from 'react';
import { AccionesReservaRecepcion } from '../../componentes/AccionesReservaRecepcion';
import { Campo } from '../../componentes/Campo';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { TarjetaReserva } from '../../componentes/TarjetaReserva';
import { FILTROS_RESERVAS_VACIOS } from '../../contexto/filtrosReservasReductor';
import { esquemaFiltrosReservas } from '../../esquemas/filtrosReservas';
import { useApartamentos } from '../../hooks/useApartamentos';
import { useConsulta } from '../../hooks/useConsulta';
import { useDebounce } from '../../hooks/useDebounce';
import { useFiltrosReservas } from '../../hooks/useFiltrosReservas';
import { CANALES_ORIGEN, ESTADOS_RESERVA, type CanalOrigen, type EstadoReserva } from '../../modelos/reserva';
import { RUTAS } from '../../rutas/rutas';
import { listarReservas, type FiltrosListadoReservas } from '../../servicios/reservaServicio';
import { hoyISO } from '../../utilidades/fechas';

const ESPERA_ESCRITURA_MS = 400;

const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_SHOW: 'No-show',
};

/**
 * Listado de reservas de recepción (GET /reservas) con filtros. Los filtros viven en el FiltrosReservasProvider
 * (useReducer): al abrir la gestión de una reserva y volver, siguen ahí. El formulario se guarda en el proveedor tras
 * una pausa de escribir (useDebounce) y cada consulta nueva cancela la anterior (AbortController). Tras cada acción
 * sobre una reserva se recarga la página actual.
 */
export default function Reservas() {
  const { estado, dispatch } = useFiltrosReservas();
  const [formulario, setFormulario] = useState<FiltrosListadoReservas>(estado.filtros);

  // Formulario → filtros (con pausa)
  const estable = useDebounce(formulario, ESPERA_ESCRITURA_MS);
  useEffect(() => {
    dispatch({ tipo: 'establecer', filtros: estable });
  }, [estable, dispatch]);

  const analisis = useMemo(() => esquemaFiltrosReservas.safeParse(formulario), [formulario]);
  const errores = useMemo(() => {
    const porCampo: Partial<Record<keyof FiltrosListadoReservas, string>> = {};
    if (!analisis.success) {
      for (const problema of analisis.error.issues) {
        const campo = problema.path[0] as keyof FiltrosListadoReservas | undefined;
        if (campo && !porCampo[campo]) porCampo[campo] = problema.message;
      }
    }
    return porCampo;
  }, [analisis]);

  // La consulta usa los filtros del proveedor (ya con pausa); si son válidos
  const consultable = esquemaFiltrosReservas.safeParse(estado.filtros);
  const clave = consultable.success ? JSON.stringify([estado.filtros, estado.pagina]) : null;
  const { resultado, cargando, recargar } = useConsulta(clave, (signal) => listarReservas(estado.filtros, estado.pagina, signal));
  const { apartamentos } = useApartamentos();

  function cambiar(campo: keyof FiltrosListadoReservas) {
    return (evento: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setFormulario((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  }

  /** Un atajo es un filtro completo: se aplica sin esperar la pausa del teclado. */
  function aplicar(filtros: FiltrosListadoReservas) {
    setFormulario(filtros);
    dispatch({ tipo: 'establecer', filtros });
  }

  return (
    <section>
      <h1 className="h3 mb-3">Reservas</h1>

      <div className="d-flex flex-wrap gap-2 mb-3">
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          onClick={() => aplicar({ ...FILTROS_RESERVAS_VACIOS, entradaDesde: hoyISO(), entradaHasta: hoyISO() })}
        >
          Llegadas de hoy
        </button>
        <button
          type="button"
          className="btn btn-outline-primary btn-sm"
          onClick={() => aplicar({ ...FILTROS_RESERVAS_VACIOS, salidaDesde: hoyISO(), salidaHasta: hoyISO() })}
        >
          Salidas de hoy
        </button>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => aplicar({ ...FILTROS_RESERVAS_VACIOS })}>
          Limpiar filtros
        </button>
      </div>

      <form noValidate onSubmit={(evento) => evento.preventDefault()} aria-label="Filtros de reservas" className="row">
        <div className="col-12 col-md-3">
          <CampoSelect id="filtroEstado" etiqueta="Estado" value={formulario.estado} onChange={cambiar('estado')} error={errores.estado}>
            <option value="">Todos</option>
            {ESTADOS_RESERVA.map((opcion) => (
              <option key={opcion} value={opcion}>{ETIQUETA_ESTADO[opcion]}</option>
            ))}
          </CampoSelect>
        </div>
        <div className="col-12 col-md-3">
          <Campo
            id="filtroNombre"
            etiqueta="Nombre del titular"
            value={formulario.nombreTitular}
            onChange={cambiar('nombreTitular')}
            error={errores.nombreTitular}
          />
        </div>
        <div className="col-12 col-md-3">
          <CampoSelect id="filtroApartamento" etiqueta="Apartamento" value={formulario.apartamento} onChange={cambiar('apartamento')} error={errores.apartamento}>
            <option value="">Todos</option>
            {apartamentos?.map((apartamento) => (
              <option key={apartamento.identificacion} value={apartamento.identificacion}>
                {apartamento.identificacion} · {apartamento.nombre}
              </option>
            ))}
          </CampoSelect>
        </div>
        <div className="col-12 col-md-3">
          <CampoSelect id="filtroCanal" etiqueta="Canal" value={formulario.canal} onChange={cambiar('canal')} error={errores.canal}>
            <option value="">Todos</option>
            {CANALES_ORIGEN.map((canal: CanalOrigen) => (
              <option key={canal} value={canal}>{canal}</option>
            ))}
          </CampoSelect>
        </div>
        <div className="col-6 col-md-3">
          <Campo id="entradaDesde" type="date" etiqueta="Entrada desde" value={formulario.entradaDesde} onChange={cambiar('entradaDesde')} error={errores.entradaDesde} />
        </div>
        <div className="col-6 col-md-3">
          <Campo id="entradaHasta" type="date" etiqueta="Entrada hasta" value={formulario.entradaHasta} onChange={cambiar('entradaHasta')} error={errores.entradaHasta} />
        </div>
        <div className="col-6 col-md-3">
          <Campo id="salidaDesde" type="date" etiqueta="Salida desde" value={formulario.salidaDesde} onChange={cambiar('salidaDesde')} error={errores.salidaDesde} />
        </div>
        <div className="col-6 col-md-3">
          <Campo id="salidaHasta" type="date" etiqueta="Salida hasta" value={formulario.salidaHasta} onChange={cambiar('salidaHasta')} error={errores.salidaHasta} />
        </div>
      </form>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">No hay reservas con esos filtros.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'reserva' : 'reservas'}, de la más reciente a la más antigua
              </p>
              {resultado.datos.contenido.map((reserva) => (
                <TarjetaReserva key={reserva.codigo} reserva={reserva} enlace={RUTAS.gestionReserva(reserva.codigo)}>
                  <AccionesReservaRecepcion codigo={reserva.codigo} estado={reserva.estado} alTerminar={recargar} />
                </TarjetaReserva>
              ))}
              <Paginacion
                paginaActual={resultado.datos.pagina}
                totalPaginas={resultado.datos.totalPaginas}
                alCambiar={(pagina) => dispatch({ tipo: 'irAPagina', pagina })}
              />
            </>
          ))}
      </div>
    </section>
  );
}
