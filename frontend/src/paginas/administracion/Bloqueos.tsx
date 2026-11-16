import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Campo } from '../../componentes/Campo';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { esquemaBloqueo, type FormularioBloqueo } from '../../esquemas/administracion';
import { useApartamentos } from '../../hooks/useApartamentos';
import { useConsulta } from '../../hooks/useConsulta';
import { listarBloqueos, registrarBloqueo } from '../../servicios/bloqueoServicio';
import { erroresDeCampo, mensajeDeError } from '../../utilidades/errores';
import { formatearRango } from '../../utilidades/formato';
import { notificarExito } from '../../utilidades/notificaciones';

const CAMPOS = ['fechaInicio', 'fechaFin', 'motivo'] as const;

/**
 * Bloqueos de un apartamento (CU-09): se elige el apartamento, se ven sus bloqueos (de 10 en 10) y se registra uno
 * nuevo por rango de fechas con su motivo. Bloquea las noches [inicio, fin): la noche del día de fin queda libre.
 * Si alguna de esas noches tiene una reserva activa el backend responde 409 y se muestra su mensaje.
 */
export default function Bloqueos() {
  const { apartamentos, error: errorApartamentos } = useApartamentos();
  const [apartamento, setApartamento] = useState('');
  const [pagina, setPagina] = useState(0);
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  const { resultado, cargando, recargar } = useConsulta(apartamento === '' ? null : `${apartamento}|${pagina}`, (signal) =>
    listarBloqueos(apartamento, pagina, signal),
  );

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioBloqueo>({
    resolver: zodResolver(esquemaBloqueo),
    mode: 'onChange',
    defaultValues: { fechaInicio: '', fechaFin: '', motivo: '' },
  });

  async function enviar(datos: FormularioBloqueo) {
    setErrorServidor(null);
    try {
      await registrarBloqueo(apartamento, datos);
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Bloqueo registrado');
    reset();
    setPagina(0);
    recargar();
  }

  return (
    <section>
      <h1 className="h3 mb-3">Bloqueos</h1>
      {errorApartamentos && <div className="alert alert-danger" role="alert">{errorApartamentos}</div>}

      <div className="row">
        <div className="col-12 col-md-6">
          <CampoSelect
            id="bloqueo-apartamento"
            etiqueta="Apartamento"
            value={apartamento}
            onChange={(evento) => {
              setApartamento(evento.target.value);
              setPagina(0);
              setErrorServidor(null);
            }}
          >
            <option value="">Elija un apartamento</option>
            {apartamentos?.map((opcion) => (
              <option key={opcion.identificacion} value={opcion.identificacion}>
                {opcion.identificacion} · {opcion.nombre}
              </option>
            ))}
          </CampoSelect>
        </div>
      </div>

      {apartamento === '' ? (
        <div className="alert alert-info">Elija un apartamento para ver y registrar sus bloqueos.</div>
      ) : (
        <div className="row g-4">
          <div className="col-12 col-lg-5">
            <h2 className="h5">Registrar un bloqueo</h2>
            {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
            <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Registrar un bloqueo">
              <div className="row">
                <div className="col-6">
                  <Campo id="fechaInicio" type="date" etiqueta="Desde (primera noche)" error={errors.fechaInicio?.message} {...register('fechaInicio', { deps: ['fechaFin'] })} />
                </div>
                <div className="col-6">
                  <Campo id="fechaFin" type="date" etiqueta="Hasta (día de salida)" error={errors.fechaFin?.message} {...register('fechaFin')} />
                </div>
              </div>
              <Campo id="motivo" etiqueta="Motivo" error={errors.motivo?.message} {...register('motivo')} />
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? 'Registrando…' : 'Registrar bloqueo'}
              </button>
            </form>
          </div>

          <div className="col-12 col-lg-7" aria-live="polite">
            <h2 className="h5">Bloqueos de {apartamento}</h2>
            {cargando && <Cargando />}
            {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
            {resultado?.tipo === 'datos' &&
              (resultado.datos.contenido.length === 0 ? (
                <div className="alert alert-secondary">Este apartamento no tiene bloqueos.</div>
              ) : (
                <>
                  {resultado.datos.contenido.map((bloqueo) => (
                    <article key={`${bloqueo.fechaInicio}-${bloqueo.fechaFin}`} className="card mb-2">
                      <div className="card-body py-2">
                        <div className="fw-semibold">{formatearRango(bloqueo.fechaInicio, bloqueo.fechaFin)}</div>
                        <div className="small text-body-secondary">{bloqueo.motivo}</div>
                      </div>
                    </article>
                  ))}
                  <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
                </>
              ))}
          </div>
        </div>
      )}
    </section>
  );
}
