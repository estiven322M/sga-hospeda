import { useState } from 'react';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { TarjetaPreparacion } from '../../componentes/TarjetaPreparacion';
import { useConsulta } from '../../hooks/useConsulta';
import type { EstadoOperativo } from '../../modelos/apartamento';
import { listarApartamentosFiltrados } from '../../servicios/apartamentoServicio';

interface SeccionProps {
  titulo: string;
  estado: EstadoOperativo;
  vacio: string;
  /** Cambia cada vez que un apartamento cambió de estado en cualquiera de las dos listas: obliga a consultar de nuevo. */
  version: number;
  alCambiar: () => void;
}

/**
 * Los apartamentos en UN estado operativo, de 10 en 10. El filtro y el corte los hace el servidor
 * (GET /apartamentos?estadoOperativo=...): aquí no se filtra ni se ordena nada en memoria.
 */
function SeccionPreparacion({ titulo, estado, vacio, version, alCambiar }: SeccionProps) {
  const [pagina, setPagina] = useState(0);
  const { resultado, ultimo, cargando } = useConsulta(`${estado}|${pagina}|${version}`, (signal) =>
    listarApartamentosFiltrados({ estadoOperativo: estado, caracteristicas: [] }, pagina, signal),
  );
  // Mientras se recarga se siguen mostrando los datos anteriores: la lista no parpadea tras cada acción
  const visible = resultado ?? ultimo;

  function alCambiarEstado() {
    setPagina(0); // el apartamento salió de esta lista: la página actual pudo quedar vacía
    alCambiar();
  }

  return (
    <section className="col-12 col-lg-6" aria-live="polite">
      <h2 className="h5">
        {titulo}
        {visible?.tipo === 'datos' && <span className="badge text-bg-light border ms-2">{visible.datos.totalElementos}</span>}
      </h2>
      {cargando && visible === null && <Cargando />}
      {visible?.tipo === 'error' && <div className="alert alert-danger" role="alert">{visible.mensaje}</div>}
      {visible?.tipo === 'datos' &&
        (visible.datos.contenido.length === 0 ? (
          <div className="alert alert-secondary">{vacio}</div>
        ) : (
          <>
            {visible.datos.contenido.map((apartamento) => (
              <TarjetaPreparacion key={apartamento.identificacion} apartamento={apartamento} alCambiar={alCambiarEstado} />
            ))}
            <Paginacion paginaActual={visible.datos.pagina} totalPaginas={visible.datos.totalPaginas} alCambiar={setPagina} />
          </>
        ))}
    </section>
  );
}

/**
 * Preparación de apartamentos (CU-10, 7.6) para el personal de servicio: los que están pendientes de preparación y los que
 * están en preparación, cada lista paginada de 10. Desde cada apartamento se inicia la preparación, se marca preparado y se
 * registran y consultan sus novedades. Tras cada cambio de estado se recargan las dos listas (el apartamento pasa de una a otra).
 */
export default function Preparacion() {
  const [version, setVersion] = useState(0);
  const recargar = () => setVersion((anterior) => anterior + 1);

  return (
    <section>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
        <h1 className="h3 mb-0">Preparación de apartamentos</h1>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={recargar}>
          Actualizar
        </button>
      </div>
      <p className="text-body-secondary">
        Un apartamento queda pendiente de preparación cuando sale su huésped o cuando administración lo reincorpora al servicio.
      </p>
      <div className="row g-4">
        <SeccionPreparacion
          titulo="Pendientes de preparación"
          estado="PENDIENTE_PREPARACION"
          vacio="No hay apartamentos pendientes de preparación."
          version={version}
          alCambiar={recargar}
        />
        <SeccionPreparacion
          titulo="En preparación"
          estado="EN_PREPARACION"
          vacio="No hay apartamentos en preparación."
          version={version}
          alCambiar={recargar}
        />
      </div>
    </section>
  );
}
