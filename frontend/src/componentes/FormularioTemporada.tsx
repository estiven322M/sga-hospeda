import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useFieldArray, useForm, useWatch } from 'react-hook-form';
import { esquemaTemporada, solicitudDeTemporada, type FormularioTemporada } from '../esquemas/administracion';
import { registrarTemporada } from '../servicios/temporadaServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { CampoCasilla } from './CampoCasilla';

/**
 * Registrar una temporada (7.4): nombre, si es la base (sin periodos: cubre el resto del año) y, si no lo es, uno o
 * más periodos de fechas. Las temporadas no pueden solaparse y solo existe una base: si se viola, el backend responde
 * 409 y se muestra su mensaje.
 */
export function FormularioTemporadaNueva({ alRegistrar }: { alRegistrar: () => void }) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioTemporada>({
    resolver: zodResolver(esquemaTemporada),
    mode: 'onChange',
    defaultValues: { nombre: '', base: false, periodos: [{ desde: '', hasta: '' }] },
  });
  const periodos = useFieldArray({ control, name: 'periodos' });
  const esBase = useWatch({ control, name: 'base' });

  async function enviar(datos: FormularioTemporada) {
    setErrorServidor(null);
    try {
      await registrarTemporada(solicitudDeTemporada(datos));
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if (campo === 'nombre') setError('nombre', { message: mensaje });
      }
      return;
    }
    notificarExito(`Temporada ${datos.nombre} registrada`);
    reset({ nombre: '', base: false, periodos: [{ desde: '', hasta: '' }] });
    alRegistrar();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Registrar una temporada">
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <Campo id="temporada-nombre" etiqueta="Nombre de la temporada" error={errors.nombre?.message} {...register('nombre')} />
      <div className="mb-3">
        <CampoCasilla id="temporada-base" etiqueta="Es la temporada base (cubre el resto del año, sin periodos)" {...register('base')} />
      </div>

      {!esBase && (
        <fieldset className="mb-3">
          <legend className="form-label fs-6">Periodos</legend>
          {periodos.fields.map((campo, indice) => (
            <div key={campo.id} className="row align-items-start">
              <div className="col-5">
                <Campo id={`periodo-${indice}-desde`} type="date" etiqueta="Desde" error={errors.periodos?.[indice]?.desde?.message} {...register(`periodos.${indice}.desde`)} />
              </div>
              <div className="col-5">
                <Campo id={`periodo-${indice}-hasta`} type="date" etiqueta="Hasta" error={errors.periodos?.[indice]?.hasta?.message} {...register(`periodos.${indice}.hasta`)} />
              </div>
              <div className="col-2">
                {periodos.fields.length > 1 && (
                  <button type="button" className="btn btn-sm btn-outline-danger mt-4" onClick={() => periodos.remove(indice)}>
                    Quitar
                  </button>
                )}
              </div>
            </div>
          ))}
          {errors.periodos?.message && <div className="text-danger small mb-2">{errors.periodos.message}</div>}
          {errors.periodos?.root?.message && <div className="text-danger small mb-2">{errors.periodos.root.message}</div>}
          <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => periodos.append({ desde: '', hasta: '' })}>
            Agregar periodo
          </button>
        </fieldset>
      )}

      <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
        {isSubmitting ? 'Registrando…' : 'Registrar temporada'}
      </button>
    </form>
  );
}
