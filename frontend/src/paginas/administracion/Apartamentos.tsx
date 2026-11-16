import { useState, type ChangeEvent } from 'react';
import { Link } from 'react-router';
import { AccionesApartamento } from '../../componentes/AccionesApartamento';
import { Cargando } from '../../componentes/Cargando';
import { CampoSelect } from '../../componentes/CampoSelect';
import { EstadoOperativoBadge } from '../../componentes/EstadoOperativoBadge';
import { EtiquetasCaracteristicas } from '../../componentes/EtiquetasCaracteristicas';
import { Paginacion } from '../../componentes/Paginacion';
import { SelectorCaracteristicas } from '../../componentes/SelectorCaracteristicas';
import { useConsulta } from '../../hooks/useConsulta';
import { ESTADOS_OPERATIVOS, type Caracteristica, type EstadoOperativo } from '../../modelos/apartamento';
import { RUTAS } from '../../rutas/rutas';
import { listarApartamentosFiltrados } from '../../servicios/apartamentoServicio';

const ETIQUETA_ESTADO: Record<EstadoOperativo, string> = {
  PREPARADO: 'Preparado',
  OCUPADO: 'Ocupado',
  PENDIENTE_PREPARACION: 'Pendiente de preparación',
  EN_PREPARACION: 'En preparación',
  FUERA_DE_SERVICIO: 'Fuera de servicio',
};

/**
 * Inventario de apartamentos (7.3) para el administrador: activos o no, de 10 en 10, con filtro por estado operativo y
 * por características (deben tenerse TODAS). Tras cada acción se recarga la página actual.
 */
export default function Apartamentos() {
  const [estadoOperativo, setEstadoOperativo] = useState<EstadoOperativo | ''>('');
  const [caracteristicas, setCaracteristicas] = useState<Caracteristica[]>([]);
  const [pagina, setPagina] = useState(0);

  const clave = JSON.stringify([estadoOperativo, caracteristicas, pagina]);
  const { resultado, cargando, recargar } = useConsulta(clave, (signal) =>
    listarApartamentosFiltrados({ estadoOperativo, caracteristicas }, pagina, signal),
  );

  function alCambiarEstado(evento: ChangeEvent<HTMLSelectElement>) {
    setEstadoOperativo(evento.target.value as EstadoOperativo | '');
    setPagina(0); // otro filtro es otra lista
  }

  function alCambiarCaracteristicas(nuevas: Caracteristica[]) {
    setCaracteristicas(nuevas);
    setPagina(0);
  }

  return (
    <section>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 className="h3 mb-0">Apartamentos</h1>
        <Link to={RUTAS.apartamentoNuevo} className="btn btn-primary">Nuevo apartamento</Link>
      </div>

      <div className="row g-4">
        <div className="col-12 col-lg-3">
          <CampoSelect id="filtroEstadoOperativo" etiqueta="Estado operativo" value={estadoOperativo} onChange={alCambiarEstado}>
            <option value="">Todos</option>
            {ESTADOS_OPERATIVOS.map((estado) => (
              <option key={estado} value={estado}>{ETIQUETA_ESTADO[estado]}</option>
            ))}
          </CampoSelect>
          <SelectorCaracteristicas prefijo="filtro" seleccionadas={caracteristicas} alCambiar={alCambiarCaracteristicas} />
          <div className="form-text">Con varias características se piden todas a la vez.</div>
        </div>

        <div className="col-12 col-lg-9" aria-live="polite">
          {cargando && <Cargando />}
          {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
          {resultado?.tipo === 'datos' &&
            (resultado.datos.contenido.length === 0 ? (
              <div className="alert alert-secondary">No hay apartamentos con esos filtros.</div>
            ) : (
              <>
                <p className="text-body-secondary small">
                  {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'apartamento' : 'apartamentos'}
                </p>
                {resultado.datos.contenido.map((apartamento) => (
                  <article key={apartamento.identificacion} className="card mb-3">
                    <div className="card-body">
                      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                        <div>
                          <h2 className="h6 mb-1">
                            {apartamento.identificacion} · {apartamento.nombre}
                          </h2>
                          <span className={`badge ${apartamento.activo ? 'text-bg-success' : 'text-bg-secondary'}`}>
                            {apartamento.activo ? 'Activo' : 'Inactivo'}
                          </span>{' '}
                          <EstadoOperativoBadge estado={apartamento.estadoOperativo} />
                        </div>
                        <div className="small text-body-secondary text-md-end">
                          Capacidad: {apartamento.capacidad} · Dormitorios: {apartamento.dormitorios}
                        </div>
                      </div>
                      <p className="small mt-2 mb-2">{apartamento.descripcion}</p>
                      <EtiquetasCaracteristicas caracteristicas={apartamento.caracteristicas} />
                      <div className="mt-3">
                        <AccionesApartamento apartamento={apartamento} alTerminar={recargar} />
                      </div>
                    </div>
                  </article>
                ))}
                <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
              </>
            ))}
        </div>
      </div>
    </section>
  );
}
