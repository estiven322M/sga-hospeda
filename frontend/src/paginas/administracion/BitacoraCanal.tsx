import { useState } from 'react';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { useConsulta } from '../../hooks/useConsulta';
import type { OperacionCanal, ResultadoCanal } from '../../modelos/canal';
import { consultarBitacora, listarTodosLosCanales } from '../../servicios/canalServicio';
import { formatearFechaHora } from '../../utilidades/formato';

const ETIQUETA_OPERACION: Record<OperacionCanal, string> = {
  CONSULTAR_DISPONIBILIDAD: 'Consultar disponibilidad',
  CREAR_RESERVA: 'Crear reserva',
  CANCELAR_RESERVA: 'Cancelar reserva',
  CONSULTAR_RESERVA: 'Consultar reserva',
};

const ETIQUETA_RESULTADO: Record<ResultadoCanal, { texto: string; clase: string }> = {
  ACEPTADA: { texto: 'Aceptada', clase: 'text-bg-success' },
  RECHAZADA: { texto: 'Rechazada', clase: 'text-bg-warning' },
  REPETIDA: { texto: 'Repetida', clase: 'text-bg-info' },
  INVALIDA: { texto: 'Inválida', clase: 'text-bg-warning' },
  NO_ENCONTRADA: { texto: 'No encontrada', clase: 'text-bg-secondary' },
  NO_AUTORIZADA: { texto: 'No autorizada', clase: 'text-bg-danger' },
  ERROR: { texto: 'Error', clase: 'text-bg-danger' },
};

/**
 * Bitácora del canal externo (7.8): cada mensaje intercambiado con un canal, con su resultado y su código HTTP, del más reciente
 * al más antiguo y de 10 en 10. Se filtra por canal y por rango de fechas (inclusivo). Una credencial desconocida no se atribuye
 * a ningún canal: solo queda en el registro del servidor.
 */
export default function BitacoraCanal() {
  const [canal, setCanal] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [pagina, setPagina] = useState(0);
  const canales = useConsulta('canales', (signal) => listarTodosLosCanales(signal));
  const { resultado, cargando, recargar } = useConsulta(`${canal}|${desde}|${hasta}|${pagina}`, (signal) =>
    consultarBitacora(canal, desde, hasta, pagina, signal),
  );

  function filtrar(cambio: () => void) {
    cambio();
    setPagina(0);
  }

  return (
    <section>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
        <h1 className="h3 mb-0">Bitácora del canal externo</h1>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={recargar} disabled={cargando}>
          Actualizar
        </button>
      </div>
      <p className="text-body-secondary">Cada mensaje que un canal envió al sistema, con su resultado y su código HTTP.</p>

      <form className="row g-2 mb-3" aria-label="Filtros de la bitácora" onSubmit={(e) => e.preventDefault()}>
        <div className="col-12 col-md-4">
          <label htmlFor="bitacora-canal" className="form-label">Canal</label>
          <select id="bitacora-canal" className="form-select" value={canal} onChange={(e) => filtrar(() => setCanal(e.target.value))}>
            <option value="">Todos los canales</option>
            {canales.resultado?.tipo === 'datos' &&
              canales.resultado.datos.map((c) => (
                <option key={c.codigo} value={c.codigo}>{c.codigo} · {c.nombre}</option>
              ))}
          </select>
        </div>
        <div className="col-6 col-md-3">
          <label htmlFor="bitacora-desde" className="form-label">Desde</label>
          <input id="bitacora-desde" type="date" className="form-control" value={desde} onChange={(e) => filtrar(() => setDesde(e.target.value))} />
        </div>
        <div className="col-6 col-md-3">
          <label htmlFor="bitacora-hasta" className="form-label">Hasta</label>
          <input id="bitacora-hasta" type="date" className="form-control" value={hasta} onChange={(e) => filtrar(() => setHasta(e.target.value))} />
        </div>
      </form>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">No hay mensajes registrados para este filtro.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'mensaje' : 'mensajes'}
              </p>
              <div className="table-responsive">
                <table className="table table-sm align-middle">
                  <thead>
                    <tr>
                      <th scope="col">Fecha</th>
                      <th scope="col">Canal</th>
                      <th scope="col">Operación</th>
                      <th scope="col">Identificador</th>
                      <th scope="col">Resultado</th>
                      <th scope="col">HTTP</th>
                      <th scope="col">Detalle</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resultado.datos.contenido.map((evento) => (
                      <tr key={`${evento.registradoEn}-${evento.canal}-${evento.operacion}-${evento.identificadorExterno ?? ''}-${evento.codigoHttp}`}>
                        <td className="text-nowrap">{formatearFechaHora(evento.registradoEn)}</td>
                        <td>{evento.canal}</td>
                        <td>{ETIQUETA_OPERACION[evento.operacion]}</td>
                        <td>{evento.identificadorExterno ?? '—'}</td>
                        <td>
                          <span className={`badge ${ETIQUETA_RESULTADO[evento.resultado].clase}`}>{ETIQUETA_RESULTADO[evento.resultado].texto}</span>
                        </td>
                        <td>{evento.codigoHttp}</td>
                        <td className="small text-body-secondary">{evento.detalle ?? ''}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
            </>
          ))}
      </div>
    </section>
  );
}
