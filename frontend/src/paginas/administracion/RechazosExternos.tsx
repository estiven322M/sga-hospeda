import { useState } from 'react';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { useConsulta } from '../../hooks/useConsulta';
import { listarRechazos } from '../../servicios/rechazoServicio';
import { formatearFechaHora, formatearRango } from '../../utilidades/formato';

/**
 * Reservas del canal externo que el sistema rechazó por conflicto (RN-18), de 10 en 10. Cada rechazo muestra el
 * motivo tal cual lo explicó el backend: es la regla que se violó (apartamento ocupado, bloqueado, estancia mínima...).
 */
export default function RechazosExternos() {
  const [pagina, setPagina] = useState(0);
  const { resultado, cargando } = useConsulta(String(pagina), (signal) => listarRechazos(pagina, signal));

  return (
    <section>
      <h1 className="h3 mb-3">Rechazos de reservas externas</h1>
      <p className="text-body-secondary">
        Reservas que llegaron por un canal externo y se rechazaron por violar una regla. Quedan como constancia.
      </p>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">No hay reservas externas rechazadas.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'rechazo' : 'rechazos'}
              </p>
              {resultado.datos.contenido.map((rechazo) => (
                <article key={`${rechazo.identificadorExterno}-${rechazo.rechazadaEn}`} className="card mb-3">
                  <div className="card-body">
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <h2 className="h6 mb-0">{rechazo.identificadorExterno}</h2>
                      <span className="badge text-bg-light border">{rechazo.canalOrigen}</span>
                    </div>
                    <ul className="list-unstyled small text-body-secondary mt-2 mb-2">
                      <li>Apartamento: {rechazo.identificacionApartamento}</li>
                      <li>Estancia solicitada: {formatearRango(rechazo.fechaEntrada, rechazo.fechaSalida)}</li>
                      <li>Rechazada: {formatearFechaHora(rechazo.rechazadaEn)}</li>
                    </ul>
                    <div className="alert alert-warning py-2 mb-0">
                      <span className="fw-semibold">Motivo:</span> {rechazo.motivo}
                    </div>
                  </div>
                </article>
              ))}
              <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
            </>
          ))}
      </div>
    </section>
  );
}
