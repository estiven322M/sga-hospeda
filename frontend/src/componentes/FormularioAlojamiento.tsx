import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaAlojamiento, type FormularioAlojamiento } from '../esquemas/administracion';
import type { AlojamientoResponse } from '../modelos/alojamiento';
import { actualizarAlojamiento } from '../servicios/alojamientoServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { CampoAreaTexto } from './CampoAreaTexto';

const CAMPOS = ['nombre', 'descripcion', 'ciudad', 'direccion', 'latitud', 'longitud', 'horaEntrada', 'horaSalida', 'normasConvivencia', 'politicaMascotas'] as const;

/** El backend manda "15:00:00"; el <input type="time"> y el esquema trabajan con "15:00". */
const aHoraCorta = (hora: string) => hora.slice(0, 5);

function valoresDe(alojamiento: AlojamientoResponse): FormularioAlojamiento {
  return {
    nombre: alojamiento.nombre,
    descripcion: alojamiento.descripcion,
    ciudad: alojamiento.ciudad,
    direccion: alojamiento.direccion,
    latitud: String(alojamiento.latitud),
    longitud: String(alojamiento.longitud),
    horaEntrada: aHoraCorta(alojamiento.horaEntrada),
    horaSalida: aHoraCorta(alojamiento.horaSalida),
    normasConvivencia: alojamiento.normasConvivencia,
    politicaMascotas: alojamiento.politicaMascotas,
  };
}

/**
 * Datos del alojamiento (7.3, L-01, L-02, L-12, L-16): identidad, ubicación, horarios, normas y política de mascotas. Zod
 * revisa la forma; las reglas (la salida anterior a la entrada, ubicación posible, mascotas definidas) las decide el
 * dominio y su 409 se muestra tal cual. Las horas editadas rigen de inmediato en las reglas de llegada y no-show.
 */
export function FormularioAlojamientoDatos({ alojamiento, alGuardar }: { alojamiento: AlojamientoResponse; alGuardar: () => void }) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<FormularioAlojamiento>({
    resolver: zodResolver(esquemaAlojamiento),
    mode: 'onChange',
    defaultValues: valoresDe(alojamiento),
  });

  async function enviar(datos: FormularioAlojamiento) {
    setErrorServidor(null);
    try {
      await actualizarAlojamiento({
        nombre: datos.nombre,
        descripcion: datos.descripcion,
        ciudad: datos.ciudad,
        direccion: datos.direccion,
        latitud: Number(datos.latitud),
        longitud: Number(datos.longitud),
        horaEntrada: datos.horaEntrada,
        horaSalida: datos.horaSalida,
        normasConvivencia: datos.normasConvivencia,
        politicaMascotas: datos.politicaMascotas,
      });
    } catch (error) {
      setErrorServidor(mensajeDeError(error)); // 409: la regla del dominio, tal cual
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Datos del alojamiento guardados');
    alGuardar();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Datos del alojamiento">
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <div className="row">
        <div className="col-12 col-md-6">
          <Campo id="aloj-nombre" etiqueta="Nombre" error={errors.nombre?.message} {...register('nombre')} />
        </div>
        <div className="col-12 col-md-6">
          <Campo id="aloj-ciudad" etiqueta="Ciudad" error={errors.ciudad?.message} {...register('ciudad')} />
        </div>
      </div>
      <CampoAreaTexto id="aloj-descripcion" etiqueta="Descripción" rows={3} error={errors.descripcion?.message} {...register('descripcion')} />
      <Campo id="aloj-direccion" etiqueta="Dirección" error={errors.direccion?.message} {...register('direccion')} />
      <div className="row">
        <div className="col-6 col-md-3">
          <Campo id="aloj-latitud" etiqueta="Latitud" inputMode="decimal" error={errors.latitud?.message} {...register('latitud')} />
        </div>
        <div className="col-6 col-md-3">
          <Campo id="aloj-longitud" etiqueta="Longitud" inputMode="decimal" error={errors.longitud?.message} {...register('longitud')} />
        </div>
        <div className="col-6 col-md-3">
          <Campo id="aloj-horaEntrada" type="time" etiqueta="Hora de entrada" error={errors.horaEntrada?.message} {...register('horaEntrada')} />
        </div>
        <div className="col-6 col-md-3">
          <Campo id="aloj-horaSalida" type="time" etiqueta="Hora de salida" error={errors.horaSalida?.message} {...register('horaSalida')} />
        </div>
      </div>
      <div className="form-text mb-3">La hora de salida debe ser anterior a la hora de entrada.</div>
      <CampoAreaTexto id="aloj-normas" etiqueta="Normas de convivencia" rows={4} error={errors.normasConvivencia?.message} {...register('normasConvivencia')} />
      <CampoAreaTexto id="aloj-mascotas" etiqueta="Política de mascotas" rows={2} error={errors.politicaMascotas?.message} {...register('politicaMascotas')} />
      <button type="submit" className="btn btn-primary" disabled={isSubmitting || !isDirty}>
        {isSubmitting ? 'Guardando…' : 'Guardar datos'}
      </button>
    </form>
  );
}
