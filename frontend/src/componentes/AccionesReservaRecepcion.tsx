import { useState } from 'react';
import { Link } from 'react-router';
import { useAutor } from '../hooks/useAutor';
import type { EstadoReserva } from '../modelos/reserva';
import { RUTAS } from '../rutas/rutas';
import {
  cancelarReserva,
  confirmarReserva,
  declararNoShow,
  obtenerReserva,
  registrarLlegada,
  registrarSalida,
} from '../servicios/reservaServicio';
import {
  puedeCancelar,
  puedeConfirmar,
  puedeDeclararNoShow,
  puedeModificar,
  puedeRegistrarLlegada,
  puedeRegistrarSalida,
} from '../utilidades/accionesReserva';
import { solicitarCancelacion } from '../utilidades/dialogosReserva';
import { mensajeDeError } from '../utilidades/errores';
import { confirmarAccion, notificarExito, pedirDato } from '../utilidades/notificaciones';

interface AccionesReservaRecepcionProps {
  codigo: string;
  estado: EstadoReserva;
  /** Se llama después de cada acción exitosa: la página que la contiene recarga lo que muestra. */
  alTerminar: () => void;
  /** Mostrar el enlace "Modificar" (lleva a la gestión de la reserva, donde está el formulario). */
  conEnlaceModificar?: boolean;
}

/**
 * Las acciones que recepción puede pedir sobre una reserva, según su estado. Antes de toda acción irreversible pide
 * confirmación; después de cada una, notifica y avisa a la página para que recargue. Si el backend rechaza la acción
 * (409: falta el anticipo, aún no es la fecha, el apartamento no está preparado...), se muestra su mensaje tal cual.
 */
export function AccionesReservaRecepcion({ codigo, estado, alTerminar, conEnlaceModificar = true }: AccionesReservaRecepcionProps) {
  const autor = useAutor();
  const [ocupado, setOcupado] = useState(false);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  /** Ejecuta la acción, y según salga: el mensaje del backend en la tarjeta, o notificación y recarga. */
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

  async function confirmar() {
    setErrorAccion(null);
    // La hora estimada de llegada es obligatoria para confirmar (RN-09): si la reserva no la tiene, se pide.
    let hora: string | undefined;
    try {
      const reserva = await obtenerReserva(codigo);
      if (reserva.horaEstimadaLlegada === null) {
        const escrita = await pedirDato({
          titulo: 'Hora estimada de llegada',
          texto: 'La reserva no tiene hora estimada de llegada y es obligatoria para confirmarla.',
          tipo: 'time',
          etiqueta: 'Hora estimada',
          confirmar: 'Continuar',
        });
        if (escrita === null) return;
        hora = escrita;
      }
    } catch (error) {
      setErrorAccion(mensajeDeError(error));
      return;
    }
    const aceptada = await confirmarAccion(
      'Confirmar la reserva',
      'Para confirmarla, lo pagado en el folio debe cubrir el anticipo que exige el alojamiento. ' +
        'Si todavía falta, el sistema le indicará el valor exacto del anticipo.',
    );
    if (!aceptada) return;
    await ejecutar(
      () => confirmarReserva(codigo, { autor, ...(hora !== undefined && { horaEstimadaLlegada: hora }) }),
      'Reserva confirmada',
    );
  }

  async function cancelar() {
    const motivo = await solicitarCancelacion();
    if (motivo === null) return;
    await ejecutar(() => cancelarReserva(codigo, { motivo, autor }), 'Reserva cancelada; la retención quedó registrada en el folio');
  }

  async function llegada() {
    const aceptada = await confirmarAccion(
      'Registrar la llegada',
      'La reserva pasará a EN CURSO y el apartamento a OCUPADO. No se puede deshacer.',
    );
    if (aceptada) await ejecutar(() => registrarLlegada(codigo, { autor }), 'Llegada registrada');
  }

  async function noShow() {
    const aceptada = await confirmarAccion(
      'Declarar no-show',
      'El huésped no se presentó: se revertirá el alojamiento y se aplicará la penalidad de la política congelada en la reserva. No se puede deshacer.',
    );
    if (aceptada) await ejecutar(() => declararNoShow(codigo, { autor }), 'No-show declarado');
  }

  async function salida() {
    const aceptada = await confirmarAccion(
      'Registrar la salida',
      'La reserva pasará a FINALIZADA y el apartamento quedará pendiente de preparación. Exige el folio cerrado. No se puede deshacer.',
    );
    if (aceptada) await ejecutar(() => registrarSalida(codigo, { autor }), 'Salida registrada');
  }

  const hayAcciones =
    puedeConfirmar(estado) || puedeModificar(estado) || puedeCancelar(estado) || puedeRegistrarLlegada(estado) || puedeRegistrarSalida(estado);
  if (!hayAcciones) return null;

  return (
    <div>
      {errorAccion && (
        <div className="alert alert-warning py-2 small" role="alert">
          {errorAccion}
        </div>
      )}
      <div className="d-flex flex-wrap gap-2">
        {puedeConfirmar(estado) && (
          <button type="button" className="btn btn-sm btn-primary" onClick={() => void confirmar()} disabled={ocupado}>
            Confirmar
          </button>
        )}
        {puedeRegistrarLlegada(estado) && (
          <button type="button" className="btn btn-sm btn-success" onClick={() => void llegada()} disabled={ocupado}>
            Registrar llegada
          </button>
        )}
        {puedeDeclararNoShow(estado) && (
          <button type="button" className="btn btn-sm btn-outline-dark" onClick={() => void noShow()} disabled={ocupado}>
            Declarar no-show
          </button>
        )}
        {puedeRegistrarSalida(estado) && (
          <button type="button" className="btn btn-sm btn-success" onClick={() => void salida()} disabled={ocupado}>
            Registrar salida
          </button>
        )}
        {conEnlaceModificar && puedeModificar(estado) && (
          <Link to={RUTAS.gestionReserva(codigo)} className="btn btn-sm btn-outline-primary">
            Modificar
          </Link>
        )}
        {puedeCancelar(estado) && (
          <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => void cancelar()} disabled={ocupado}>
            Cancelar
          </button>
        )}
      </div>
    </div>
  );
}
