import { useAlojamiento } from '../hooks/useAlojamiento';
import { formatearHora } from '../utilidades/formato';

/**
 * Horarios, normas de convivencia y política de mascotas del alojamiento, tal como los entrega GET /alojamiento (público).
 * Si la consulta falla, no se muestra nada: la página que lo contiene no depende de estos datos.
 */
export function InformacionAlojamiento() {
  const { alojamiento } = useAlojamiento();
  if (!alojamiento) return null;

  return (
    <section aria-label="Información del alojamiento" className="card">
      <div className="card-body">
        <h2 className="h5 card-title">{alojamiento.nombre}</h2>
        <p className="text-body-secondary small mb-3">
          {alojamiento.direccion} · {alojamiento.ciudad}
        </p>
        <dl className="row small mb-0">
          <dt className="col-sm-3">Horarios</dt>
          <dd className="col-sm-9">
            Entrada desde las {formatearHora(alojamiento.horaEntrada)} · Salida hasta las {formatearHora(alojamiento.horaSalida)}
          </dd>
          <dt className="col-sm-3">Normas de convivencia</dt>
          <dd className="col-sm-9">{alojamiento.normasConvivencia}</dd>
          <dt className="col-sm-3">Mascotas</dt>
          <dd className="col-sm-9 mb-0">{alojamiento.politicaMascotas}</dd>
        </dl>
      </div>
    </section>
  );
}
