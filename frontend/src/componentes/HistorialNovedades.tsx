import { useState } from 'react';
import { useConsulta } from '../hooks/useConsulta';
import { listarNovedades } from '../servicios/novedadServicio';
import { formatearFechaHora } from '../utilidades/formato';
import { Cargando } from './Cargando';
import { GravedadBadge } from './GravedadBadge';
import { Paginacion } from './Paginacion';

/**
 * Historial de novedades de un apartamento (7.6): fecha, autor, descripción y gravedad, de la más reciente a la más antigua y de
 * 10 en 10 (el orden y el corte los hace el servidor). Lo usan el personal de servicio y el administrador.
 */
export function HistorialNovedades({ identificacion }: { identificacion: string }) {
  const [pagina, setPagina] = useState(0);
  const { resultado, cargando } = useConsulta(`${identificacion}|${pagina}`, (signal) =>
    listarNovedades(identificacion, pagina, signal),
  );

  return (
    <div aria-live="polite" aria-label={`Historial de novedades de ${identificacion}`}>
      <h3 className="h6">Historial de novedades</h3>
      {cargando && <Cargando />}
      {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
      {resultado?.tipo === 'datos' &&
        (resultado.datos.contenido.length === 0 ? (
          <p className="text-body-secondary small mb-0">Este apartamento no tiene novedades registradas.</p>
        ) : (
          <>
            <ul className="list-group list-group-flush mb-2">
              {resultado.datos.contenido.map((novedad, indice) => (
                <li key={`${novedad.fecha}-${indice}`} className="list-group-item px-0">
                  <div className="d-flex flex-wrap justify-content-between gap-2 small text-body-secondary">
                    <span>
                      {formatearFechaHora(novedad.fecha)} · {novedad.autor}
                    </span>
                    <GravedadBadge gravedad={novedad.gravedad} />
                  </div>
                  <p className="mb-0">{novedad.descripcion}</p>
                </li>
              ))}
            </ul>
            <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
          </>
        ))}
    </div>
  );
}
