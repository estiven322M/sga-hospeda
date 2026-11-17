import { useState } from 'react';
import type { ApartamentoResponse } from '../modelos/apartamento';
import { iniciarPreparacion, marcarPreparado } from '../servicios/apartamentoServicio';
import { mensajeDeError } from '../utilidades/errores';
import { confirmarAccion, notificarExito } from '../utilidades/notificaciones';
import { EstadoOperativoBadge } from './EstadoOperativoBadge';
import { FormularioNovedad } from './FormularioNovedad';
import { HistorialNovedades } from './HistorialNovedades';

interface TarjetaPreparacionProps {
  apartamento: ApartamentoResponse;
  /** Se llama cuando el estado operativo cambió: la página recarga sus listas. */
  alCambiar: () => void;
}

type Panel = 'novedad' | 'historial' | null;

/**
 * Un apartamento por preparar (CU-10, 7.6): su estado operativo, la transición que le toca ("Iniciar preparación" o "Marcar
 * preparado", con confirmación) y sus novedades (registrar e historial). Qué transición es válida lo decide el backend: si la
 * rechaza (409) se muestra su mensaje. Aquí no hay "fuera de servicio" ni bloqueos: son decisiones del administrador.
 */
export function TarjetaPreparacion({ apartamento, alCambiar }: TarjetaPreparacionProps) {
  const { identificacion, nombre, estadoOperativo } = apartamento;
  const [ocupado, setOcupado] = useState(false);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);
  const [panel, setPanel] = useState<Panel>(null);

  async function ejecutar(titulo: string, texto: string, accion: () => Promise<unknown>, exito: string) {
    if (!(await confirmarAccion(titulo, texto))) return;
    setErrorAccion(null);
    setOcupado(true);
    try {
      await accion();
    } catch (error) {
      setErrorAccion(mensajeDeError(error));
      return;
    } finally {
      setOcupado(false);
    }
    notificarExito(exito);
    alCambiar();
  }

  const alternar = (elegido: Exclude<Panel, null>) => setPanel((actual) => (actual === elegido ? null : elegido));

  return (
    <article className="card mb-3">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h3 className="h6 mb-1">
              {identificacion} · {nombre}
            </h3>
            <EstadoOperativoBadge estado={estadoOperativo} />
          </div>
          <div className="small text-body-secondary text-md-end">
            Capacidad: {apartamento.capacidad} · Dormitorios: {apartamento.dormitorios}
          </div>
        </div>

        {errorAccion && (
          <div className="alert alert-warning py-2 small mt-3 mb-0" role="alert">{errorAccion}</div>
        )}

        <div className="d-flex flex-wrap gap-2 mt-3">
          {estadoOperativo === 'PENDIENTE_PREPARACION' && (
            <button
              type="button"
              className="btn btn-sm btn-primary"
              disabled={ocupado}
              onClick={() =>
                void ejecutar(
                  'Iniciar preparación',
                  `El apartamento ${identificacion} pasará a "En preparación".`,
                  () => iniciarPreparacion(identificacion),
                  `Preparación de ${identificacion} iniciada`,
                )
              }
            >
              Iniciar preparación
            </button>
          )}
          {estadoOperativo === 'EN_PREPARACION' && (
            <button
              type="button"
              className="btn btn-sm btn-success"
              disabled={ocupado}
              onClick={() =>
                void ejecutar(
                  'Marcar preparado',
                  `El apartamento ${identificacion} quedará "Preparado" y podrá recibir huéspedes.`,
                  () => marcarPreparado(identificacion),
                  `${identificacion} quedó preparado`,
                )
              }
            >
              Marcar preparado
            </button>
          )}
          <button type="button" className="btn btn-sm btn-outline-primary" aria-expanded={panel === 'novedad'} onClick={() => alternar('novedad')}>
            Registrar novedad
          </button>
          <button type="button" className="btn btn-sm btn-outline-secondary" aria-expanded={panel === 'historial'} onClick={() => alternar('historial')}>
            Historial de novedades
          </button>
        </div>

        {panel === 'novedad' && (
          <div className="border-top mt-3 pt-3">
            {/* Al registrar se pasa al historial: se monta de nuevo y trae la novedad recién creada */}
            <FormularioNovedad identificacion={identificacion} alRegistrar={() => setPanel('historial')} alCancelar={() => setPanel(null)} />
          </div>
        )}
        {panel === 'historial' && (
          <div className="border-top mt-3 pt-3">
            <HistorialNovedades identificacion={identificacion} />
          </div>
        )}
      </div>
    </article>
  );
}
