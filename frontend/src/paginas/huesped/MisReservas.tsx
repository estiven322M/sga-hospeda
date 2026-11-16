import { useState, type ChangeEvent } from 'react';
import { Link } from 'react-router';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { TarjetaReserva } from '../../componentes/TarjetaReserva';
import { useConsulta } from '../../hooks/useConsulta';
import { ESTADOS_RESERVA, type EstadoReserva } from '../../modelos/reserva';
import { RUTAS } from '../../rutas/rutas';
import { listarMisReservas } from '../../servicios/reservaServicio';

const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_SHOW: 'No-show',
};

/**
 * Las reservas del huésped que ingresó (GET /reservas/mias): el titular sale del token en el backend, así que nadie
 * puede pedir las de otra persona. Páginas de 10, de la más reciente a la más antigua; filtro opcional por estado.
 */
export default function MisReservas() {
  const [estado, setEstado] = useState<EstadoReserva | ''>('');
  const [pagina, setPagina] = useState(0);

  const { resultado, cargando } = useConsulta(`${estado}|${pagina}`, (signal) => listarMisReservas(estado, pagina, signal));

  function alCambiarEstado(evento: ChangeEvent<HTMLSelectElement>) {
    setEstado(evento.target.value as EstadoReserva | '');
    setPagina(0); // otro filtro es otra lista: se vuelve a la primera página
  }

  return (
    <section>
      <h1 className="h3 mb-3">Mis reservas</h1>

      <div className="row mb-3">
        <div className="col-12 col-md-4">
          <CampoSelect id="filtroEstado" etiqueta="Estado" value={estado} onChange={alCambiarEstado}>
            <option value="">Todos</option>
            {ESTADOS_RESERVA.map((opcion) => (
              <option key={opcion} value={opcion}>{ETIQUETA_ESTADO[opcion]}</option>
            ))}
          </CampoSelect>
        </div>
      </div>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && (
          <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>
        )}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">
              {estado === '' ? 'Aún no tiene reservas.' : 'No tiene reservas en ese estado.'}{' '}
              <Link to={RUTAS.buscar}>Buscar un apartamento</Link>
            </div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'reserva' : 'reservas'}, de la más reciente a la más antigua
              </p>
              {resultado.datos.contenido.map((reserva) => (
                <TarjetaReserva key={reserva.codigo} reserva={reserva} enlace={RUTAS.detalleReserva(reserva.codigo)} />
              ))}
              <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
            </>
          ))}
      </div>
    </section>
  );
}
