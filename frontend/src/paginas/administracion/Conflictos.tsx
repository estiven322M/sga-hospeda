import { useState } from 'react';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { useConsulta } from '../../hooks/useConsulta';
import type { FiltroRevision, RechazoReservaExternaResponse } from '../../modelos/rechazo';
import { listarRechazos, resolverRechazo } from '../../servicios/rechazoServicio';
import { mensajeDeError } from '../../utilidades/errores';
import { formatearFechaHora, formatearRango } from '../../utilidades/formato';
import { notificarError, notificarExito, pedirDato } from '../../utilidades/notificaciones';

const FILTROS: { valor: FiltroRevision; etiqueta: string }[] = [
  { valor: 'PENDIENTES', etiqueta: 'Pendientes' },
  { valor: 'REVISADOS', etiqueta: 'Revisados' },
  { valor: 'TODOS', etiqueta: 'Todos' },
];

/** Longitud máxima de la nota de revisión (ResolverRechazoRequest). */
const MAXIMO_NOTA = 500;

/**
 * Conflictos del canal externo (RN-18, 7.8): reservas que llegaron por un canal y se rechazaron porque chocaban con una reserva
 * vigente u otra regla. El motivo es el del backend, tal cual. El administrador los revisa con una NOTA: queda quién y cuándo.
 * Resolver un conflicto no toca la reserva vigente con la que chocó.
 */
export default function Conflictos() {
  const [filtro, setFiltro] = useState<FiltroRevision>('PENDIENTES');
  const [pagina, setPagina] = useState(0);
  const [ocupado, setOcupado] = useState<number | null>(null);
  const { resultado, cargando, recargar } = useConsulta(`${filtro}-${pagina}`, (signal) => listarRechazos(filtro, pagina, signal));

  function cambiarFiltro(nuevo: FiltroRevision) {
    setFiltro(nuevo);
    setPagina(0);
  }

  async function resolver(rechazo: RechazoReservaExternaResponse) {
    const nota = await pedirDato({
      titulo: `Resolver el conflicto de ${rechazo.identificadorExterno}`,
      texto: 'Escriba qué se revisó y qué se decidió. Queda guardada con su nombre y la fecha; la reserva vigente no se modifica.',
      tipo: 'textarea',
      etiqueta: 'Nota de revisión',
      confirmar: 'Marcar como revisado',
      maximo: MAXIMO_NOTA,
    });
    if (nota === null) return;
    setOcupado(rechazo.id);
    try {
      await resolverRechazo(rechazo.id, nota);
      notificarExito('Conflicto marcado como revisado');
      recargar();
    } catch (error) {
      notificarError(mensajeDeError(error));
    } finally {
      setOcupado(null);
    }
  }

  return (
    <section>
      <h1 className="h3 mb-3">Conflictos del canal externo</h1>
      <p className="text-body-secondary">
        Reservas que llegaron por un canal externo y se rechazaron por violar una regla. El administrador las revisa y deja una nota.
      </p>

      <div className="btn-group mb-3" role="group" aria-label="Filtrar por revisión">
        {FILTROS.map(({ valor, etiqueta }) => (
          <button
            key={valor}
            type="button"
            className={`btn btn-sm ${filtro === valor ? 'btn-primary' : 'btn-outline-primary'}`}
            aria-pressed={filtro === valor}
            onClick={() => cambiarFiltro(valor)}
          >
            {etiqueta}
          </button>
        ))}
      </div>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">
              {filtro === 'PENDIENTES' ? 'No hay conflictos pendientes de revisión.' : 'No hay conflictos para este filtro.'}
            </div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'conflicto' : 'conflictos'}
              </p>
              {resultado.datos.contenido.map((rechazo) => (
                <article key={rechazo.id} className="card mb-3">
                  <div className="card-body">
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <h2 className="h6 mb-0">{rechazo.identificadorExterno}</h2>
                      <div className="d-flex gap-1">
                        <span className="badge text-bg-light border">{rechazo.canal ?? rechazo.canalOrigen}</span>
                        <span className={`badge ${rechazo.revisado ? 'text-bg-success' : 'text-bg-warning'}`}>
                          {rechazo.revisado ? 'Revisado' : 'Pendiente'}
                        </span>
                      </div>
                    </div>
                    <ul className="list-unstyled small text-body-secondary mt-2 mb-2">
                      <li>Apartamento: {rechazo.identificacionApartamento}</li>
                      <li>Estancia solicitada: {formatearRango(rechazo.fechaEntrada, rechazo.fechaSalida)}</li>
                      <li>Rechazado: {formatearFechaHora(rechazo.rechazadaEn)}</li>
                    </ul>
                    <div className="alert alert-warning py-2 mb-2">
                      <span className="fw-semibold">Motivo:</span> {rechazo.motivo}
                    </div>
                    {rechazo.revisado && rechazo.revisadoEn ? (
                      <div className="alert alert-success py-2 mb-0">
                        <span className="fw-semibold">Revisión:</span> {rechazo.notaRevision}
                        <div className="small">
                          {rechazo.revisadoPor} · {formatearFechaHora(rechazo.revisadoEn)}
                        </div>
                      </div>
                    ) : (
                      <button type="button" className="btn btn-outline-primary btn-sm" disabled={ocupado === rechazo.id} onClick={() => resolver(rechazo)}>
                        Resolver
                      </button>
                    )}
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
