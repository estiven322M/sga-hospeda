import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { Cargando } from '../../componentes/Cargando';
import { DatosReserva } from '../../componentes/DatosReserva';
import { FolioDetalle } from '../../componentes/FolioDetalle';
import { useAutor } from '../../hooks/useAutor';
import { useConsulta } from '../../hooks/useConsulta';
import { RUTAS } from '../../rutas/rutas';
import { puedeCancelar } from '../../utilidades/accionesReserva';
import { solicitarCancelacion } from '../../utilidades/dialogosReserva';
import { mensajeDeError } from '../../utilidades/errores';
import { formatearPesos } from '../../utilidades/formato';
import { notificarExito } from '../../utilidades/notificaciones';
import { cancelarReserva, obtenerReserva } from '../../servicios/reservaServicio';
import { obtenerFolio } from '../../servicios/folioServicio';

/**
 * Detalle de una reserva del huésped: datos, ocupantes, historial y folio. Si el estado lo permite puede cancelarla;
 * el backend aplica la política de cancelación congelada en la reserva y registra la retención como cargo en el folio,
 * que se recarga para mostrarla. Un 403 (la reserva es de otra persona) lo atiende el interceptor de Axios.
 */
export default function DetalleReserva() {
  const { codigo = '' } = useParams();
  const autor = useAutor();
  const [cancelando, setCancelando] = useState(false);
  const [errorCancelacion, setErrorCancelacion] = useState<string | null>(null);

  const reserva = useConsulta(`reserva|${codigo}`, (signal) => obtenerReserva(codigo, signal));
  const folio = useConsulta(`folio|${codigo}`, (signal) => obtenerFolio(codigo, signal));

  async function cancelar() {
    setErrorCancelacion(null);
    const motivo = await solicitarCancelacion();
    if (motivo === null) return;
    setCancelando(true);
    try {
      await cancelarReserva(codigo, { motivo, autor });
    } catch (error) {
      setErrorCancelacion(mensajeDeError(error));
      return;
    } finally {
      setCancelando(false);
    }
    notificarExito('Reserva cancelada');
    reserva.recargar();
    folio.recargar(); // la retención es un cargo nuevo del folio
  }

  if (reserva.cargando) return <Cargando />;
  if (reserva.resultado?.tipo === 'error') {
    return (
      <section>
        <h1 className="h3">Detalle de la reserva</h1>
        <div className="alert alert-danger" role="alert">
          {reserva.resultado.estadoHttp === 404 ? `No existe la reserva ${codigo}.` : reserva.resultado.mensaje}
        </div>
        <Link to={RUTAS.misReservas} className="btn btn-outline-primary">Volver a mis reservas</Link>
      </section>
    );
  }
  if (reserva.resultado === null) return <Cargando />;
  const datos = reserva.resultado.datos;
  const datosFolio = folio.resultado?.tipo === 'datos' ? folio.resultado.datos : null;
  // La retención de la cancelación es un cargo del folio; si no hay, la cancelación no retuvo nada.
  const retencion = datosFolio?.cargos.find((cargo) => cargo.concepto.startsWith('Penalidad'));

  return (
    <section>
      <h1 className="h3 mb-3">Detalle de la reserva</h1>
      <DatosReserva reserva={datos} />

      {datos.estado === 'CANCELADA' && datosFolio && (
        <div className="alert alert-info" role="status">
          {retencion
            ? `Retención aplicada por la cancelación: ${formatearPesos(retencion.valor)} (política de cancelación versión ${datos.versionPolitica}). El resto del alojamiento quedó como saldo a favor en el folio.`
            : `La cancelación no tuvo retención (política de cancelación versión ${datos.versionPolitica}).`}
        </div>
      )}

      <div className="mt-4">
        {folio.cargando && <Cargando />}
        {folio.resultado?.tipo === 'error' && <div className="alert alert-warning" role="alert">{folio.resultado.mensaje}</div>}
        {datosFolio && <FolioDetalle folio={datosFolio} />}
      </div>

      {errorCancelacion && <div className="alert alert-warning" role="alert">{errorCancelacion}</div>}
      <div className="d-flex gap-2 mt-3">
        {puedeCancelar(datos.estado) && (
          <button type="button" className="btn btn-outline-danger" onClick={() => void cancelar()} disabled={cancelando}>
            {cancelando ? 'Cancelando…' : 'Cancelar reserva'}
          </button>
        )}
        <Link to={RUTAS.misReservas} className="btn btn-outline-secondary">Volver a mis reservas</Link>
      </div>
    </section>
  );
}
