import { useState } from 'react';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { FormularioTemporadaNueva } from '../../componentes/FormularioTemporada';
import { Paginacion } from '../../componentes/Paginacion';
import { PanelTarifas } from '../../componentes/PanelTarifas';
import { useApartamentos } from '../../hooks/useApartamentos';
import { useConsulta } from '../../hooks/useConsulta';
import { listarTemporadas, listarTodasLasTemporadas } from '../../servicios/temporadaServicio';
import { formatearFecha } from '../../utilidades/formato';

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
      <h1 className="h3 mb-3">Temporadas y tarifas</h1>

      <div className="row g-4 mb-5">
        <div className="col-12 col-lg-7" aria-live="polite">
          <h2 className="h5">Temporadas</h2>
          {visibles.cargando && <Cargando />}
          {visibles.resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{visibles.resultado.mensaje}</div>}
          {visibles.resultado?.tipo === 'datos' &&
            (visibles.resultado.datos.contenido.length === 0 ? (
              <div className="alert alert-secondary">Aún no hay temporadas registradas.</div>
            ) : (
              <>
                {visibles.resultado.datos.contenido.map((temporada) => (
                  <article key={temporada.nombre} className="card mb-2">
                    <div className="card-body py-2">
                      <div className="fw-semibold">
                        {temporada.nombre} {temporada.base && <span className="badge text-bg-light border">Base</span>}
                      </div>
                      <div className="small text-body-secondary">
                        {temporada.periodos.length === 0
                          ? 'Cubre el resto del año'
                          : temporada.periodos.map((p) => `${formatearFecha(p.desde)} → ${formatearFecha(p.hasta)}`).join(' · ')}
                      </div>
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
          <h2 className="h5">Registrar una temporada</h2>
          <FormularioTemporadaNueva alRegistrar={temporadaRegistrada} />
        </div>
      </div>

      <h2 className="h5">Tarifas de un apartamento</h2>
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
