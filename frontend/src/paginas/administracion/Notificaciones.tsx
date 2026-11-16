import { useState } from 'react';
import { Link } from 'react-router';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { useConsulta } from '../../hooks/useConsulta';
import type { EstadoNotificacion, TipoNotificacion } from '../../modelos/notificacion';
import { RUTAS } from '../../rutas/rutas';
import { listarNotificaciones } from '../../servicios/notificacionServicio';
import { formatearFechaHora } from '../../utilidades/formato';

const ETIQUETA_TIPO: Record<TipoNotificacion, string> = {
  RESERVA_CREADA: 'Reserva creada',
  RESERVA_CONFIRMADA: 'Reserva confirmada',
  RESERVA_CANCELADA: 'Reserva cancelada',
  CODIGO_RECUPERACION: 'Código de recuperación',
};

const ETIQUETA_ESTADO: Record<EstadoNotificacion, { texto: string; clase: string }> = {
  ENVIADA: { texto: 'Enviada', clase: 'text-bg-success' },
  FALLIDA: { texto: 'Fallida', clase: 'text-bg-danger' },
  SIN_CORREO: { texto: 'Sin correo', clase: 'text-bg-warning' },
};

/**
 * Bandeja de notificaciones (integración de salida, 10.3.2-A): los avisos por correo que el sistema intentó enviar al titular,
 * de 10 en 10 y del más reciente al más antiguo. Con el proveedor local (el que viene activo) es el buzón de demostración y
 * muestra el contenido completo, incluido el código de recuperación; con el correo real muestra si cada envío salió o falló.
 * Un fallo de envío nunca impidió reservar, confirmar ni cobrar: solo queda aquí y en el registro del servidor.
 */
export default function Notificaciones() {
  const [pagina, setPagina] = useState(0);
  const { resultado, cargando, recargar } = useConsulta(String(pagina), (signal) => listarNotificaciones(pagina, signal));

  return (
    <section>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
        <h1 className="h3 mb-0">Notificaciones</h1>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={recargar} disabled={cargando}>
          Actualizar
        </button>
      </div>
      <p className="text-body-secondary">
        Avisos por correo al titular (reserva creada, confirmada y cancelada) y códigos de recuperación de contraseña. Esta bandeja
        está en la memoria del servidor: se vacía cuando se reinicia.
      </p>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">Aún no se ha enviado ninguna notificación.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'aviso' : 'avisos'}
              </p>
              {resultado.datos.contenido.map((aviso) => (
                <article key={`${aviso.registradaEn}-${aviso.tipo}-${aviso.destinatario ?? ''}-${aviso.codigoReserva ?? ''}`} className="card mb-3">
                  <div className="card-body">
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <h2 className="h6 mb-0">{aviso.asunto}</h2>
                      <div className="d-flex gap-1">
                        <span className="badge text-bg-light border">{ETIQUETA_TIPO[aviso.tipo]}</span>
                        <span className={`badge ${ETIQUETA_ESTADO[aviso.estado].clase}`}>{ETIQUETA_ESTADO[aviso.estado].texto}</span>
                      </div>
                    </div>
                    <ul className="list-unstyled small text-body-secondary mt-2 mb-2">
                      <li>Para: {aviso.destinatario ?? 'sin correo conocido'}</li>
                      <li>Registrado: {formatearFechaHora(aviso.registradaEn)}</li>
                      {aviso.codigoReserva && (
                        <li>
                          Reserva: <Link to={RUTAS.gestionReserva(aviso.codigoReserva)}>{aviso.codigoReserva}</Link>
                        </li>
                      )}
                    </ul>
                    <p className="mb-2" style={{ whiteSpace: 'pre-wrap' }}>{aviso.mensaje}</p>
                    {aviso.detalle && <div className="alert alert-warning py-2 mb-0">{aviso.detalle}</div>}
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
