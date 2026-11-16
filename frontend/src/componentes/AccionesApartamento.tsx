import { useState } from 'react';
import { Link } from 'react-router';
import type { ApartamentoResponse } from '../modelos/apartamento';
import { RUTAS } from '../rutas/rutas';
import {
  activarApartamento,
  darDeBajaApartamento,
  declararFueraDeServicio,
  reincorporarApartamento,
} from '../servicios/apartamentoServicio';
import { mensajeDeError } from '../utilidades/errores';
import { confirmarAccion, notificarExito } from '../utilidades/notificaciones';

interface AccionesApartamentoProps {
  apartamento: ApartamentoResponse;
  /** Se llama tras cada acción exitosa: la página recarga la página actual del listado. */
  alTerminar: () => void;
}

/**
 * Las acciones de administración sobre un apartamento. Qué se ofrece depende de su estado, pero quien decide si la
 * acción es válida es el backend: activar sin tarifa en todas las temporadas responde 409 y se muestra su mensaje.
 */
export function AccionesApartamento({ apartamento, alTerminar }: AccionesApartamentoProps) {
  const { identificacion, activo, estadoOperativo } = apartamento;
  const [ocupado, setOcupado] = useState(false);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  async function ejecutar(accion: () => Promise<unknown>, exito: string) {
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
    alTerminar();
  }

  async function darDeBaja() {
    const aceptada = await confirmarAccion(
      'Dar de baja el apartamento',
      'El apartamento dejará de venderse. Es una baja lógica: el registro y sus reservas anteriores se conservan, y se puede volver a activar.',
    );
    if (aceptada) await ejecutar(() => darDeBajaApartamento(identificacion), 'Apartamento dado de baja');
  }

  async function fueraDeServicio() {
    const aceptada = await confirmarAccion(
      'Declarar fuera de servicio',
      'Es una decisión administrativa: el apartamento no se podrá preparar ni usar hasta que se reincorpore. No crea un bloqueo de fechas.',
    );
    if (aceptada) await ejecutar(() => declararFueraDeServicio(identificacion), 'Apartamento fuera de servicio');
  }

  return (
    <div>
      {errorAccion && (
        <div className="alert alert-warning py-2 small" role="alert">{errorAccion}</div>
      )}
      <div className="d-flex flex-wrap gap-2">
        <Link to={RUTAS.apartamentoEditar(identificacion)} className="btn btn-sm btn-outline-primary">Editar</Link>
        {!activo && (
          <button type="button" className="btn btn-sm btn-success" onClick={() => void ejecutar(() => activarApartamento(identificacion), 'Apartamento activado')} disabled={ocupado}>
            Activar
          </button>
        )}
        {activo && (
          <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => void darDeBaja()} disabled={ocupado}>
            Dar de baja
          </button>
        )}
        {estadoOperativo !== 'FUERA_DE_SERVICIO' && estadoOperativo !== 'OCUPADO' && (
          <button type="button" className="btn btn-sm btn-outline-dark" onClick={() => void fueraDeServicio()} disabled={ocupado}>
            Declarar fuera de servicio
          </button>
        )}
        {estadoOperativo === 'FUERA_DE_SERVICIO' && (
          <button type="button" className="btn btn-sm btn-outline-success" onClick={() => void ejecutar(() => reincorporarApartamento(identificacion), 'Apartamento reincorporado')} disabled={ocupado}>
            Reincorporar
          </button>
        )}
      </div>
    </div>
  );
}
