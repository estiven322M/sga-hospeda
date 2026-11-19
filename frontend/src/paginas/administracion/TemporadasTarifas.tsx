import { useState } from 'react';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { FormularioTemporadaNueva } from '../../componentes/FormularioTemporada';
import { Paginacion } from '../../componentes/Paginacion';
import { PanelTarifas } from '../../componentes/PanelTarifas';
import { PeriodosTemporada } from '../../componentes/PeriodosTemporada';
import { useApartamentos } from '../../hooks/useApartamentos';
import { useConsulta } from '../../hooks/useConsulta';
import { listarTemporadas, listarTodasLasTemporadas } from '../../servicios/temporadaServicio';

/**
 * Temporadas y tarifas (7.4). Arriba, las temporadas (de 10 en 10) y el formulario para registrar una; abajo, las
 * tarifas de un apartamento en cada temporada, con las que faltan marcadas.
 */
export default function TemporadasTarifas() {
  const [pagina, setPagina] = useState(0);
  const [apartamento, setApartamento] = useState('');
  const { apartamentos } = useApartamentos();

  const visibles = useConsulta(`pagina|${pagina}`, (signal) => listarTemporadas(pagina, signal));
  // Todas las temporadas, para cruzarlas con las tarifas (el listado visible está paginado)
  const todas = useConsulta('todas', (signal) => listarTodasLasTemporadas(signal));

  function temporadaRegistrada() {
    setPagina(0);
    visibles.recargar();
    todas.recargar();
  }

  return (
    <section>
      <div className="mb-4">
        <h1 className="h3 mb-1">Temporadas y tarifas</h1>
        <p className="text-body-secondary mb-0">
          Las temporadas definen qué tarifa se cobra cada noche; cada apartamento necesita tarifa en todas.
        </p>
      </div>

      <div className="row g-4 mb-5">
        <div className="col-12 col-lg-7" aria-live="polite">
          <h2 className="h5 seccion-titulo">Temporadas</h2>
          {visibles.cargando && <Cargando />}
          {visibles.resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{visibles.resultado.mensaje}</div>}
          {visibles.resultado?.tipo === 'datos' &&
            (visibles.resultado.datos.contenido.length === 0 ? (
              <div className="alert alert-secondary">Aún no hay temporadas registradas.</div>
            ) : (
              <>
                {visibles.resultado.datos.contenido.map((temporada) => (
                  <article
                    key={temporada.nombre}
                    className={`card mb-3 temporada ${temporada.base ? 'temporada--base' : ''}`}
                    data-temporada={temporada.nombre.toLowerCase()}
                  >
                    <div className="card-body">
                      <div className="d-flex align-items-center justify-content-between gap-2 mb-2">
                        <div className="temporada__nombre">
                          {temporada.nombre} {temporada.base && <span className="badge text-bg-light border">Base</span>}
                        </div>
                        {temporada.periodos.length > 0 && (
                          <span className="temporada__conteo">
                            {temporada.periodos.length} {temporada.periodos.length === 1 ? 'periodo' : 'periodos'}
                          </span>
                        )}
                      </div>
                      <PeriodosTemporada periodos={temporada.periodos} textoSinPeriodos="Cubre el resto del año" />
                    </div>
                  </article>
                ))}
                <Paginacion
                  paginaActual={visibles.resultado.datos.pagina}
                  totalPaginas={visibles.resultado.datos.totalPaginas}
                  alCambiar={setPagina}
                />
              </>
            ))}
        </div>
        <div className="col-12 col-lg-5">
          <div className="panel-filtros">
            <h2 className="h5 mb-3">Registrar una temporada</h2>
            <FormularioTemporadaNueva alRegistrar={temporadaRegistrada} />
          </div>
        </div>
      </div>

      <h2 className="h5 seccion-titulo">Tarifas de un apartamento</h2>
      <div className="row">
        <div className="col-12 col-md-6">
          <CampoSelect id="tarifas-apartamento" etiqueta="Apartamento" value={apartamento} onChange={(evento) => setApartamento(evento.target.value)}>
            <option value="">Elija un apartamento</option>
            {apartamentos?.map((opcion) => (
              <option key={opcion.identificacion} value={opcion.identificacion}>
                {opcion.identificacion} · {opcion.nombre}
              </option>
            ))}
          </CampoSelect>
        </div>
      </div>
      {apartamento === '' ? (
        <div className="alert alert-info">Elija un apartamento para ver y registrar su tarifa en cada temporada.</div>
      ) : todas.cargando ? (
        <Cargando />
      ) : todas.resultado?.tipo === 'datos' ? (
        <PanelTarifas key={`${apartamento}-${todas.resultado.datos.length}`} identificacion={apartamento} temporadas={todas.resultado.datos} />
      ) : null}
    </section>
  );
}
