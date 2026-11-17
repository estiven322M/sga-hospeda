import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaNovedad, type FormularioNovedad as DatosNovedad } from '../esquemas/novedad';
import { GRAVEDADES } from '../modelos/novedad';
import { registrarNovedad } from '../servicios/novedadServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { CampoAreaTexto } from './CampoAreaTexto';
import { CampoSelect } from './CampoSelect';
import { ETIQUETA_GRAVEDAD } from '../utilidades/etiquetas';

const CAMPOS = ['descripcion', 'gravedad'] as const;
const MAXIMO_DESCRIPCION = 500;

interface FormularioNovedadProps {
  /** Apartamento sobre el que se registra la novedad. */
  identificacion: string;
  /** Se llama cuando el backend la registró (201). */
  alRegistrar: () => void;
  alCancelar: () => void;
}

/**
 * Registrar una novedad sobre un apartamento (7.6): descripción y gravedad. El autor y la fecha NO se piden: el backend toma
 * el autor del token y la fecha de su reloj. Un 400 pinta el error en su campo; un 409 muestra el mensaje de la regla.
 */
export function FormularioNovedad({ identificacion, alRegistrar, alCancelar }: FormularioNovedadProps) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosNovedad>({
    resolver: zodResolver(esquemaNovedad),
    mode: 'onChange',
    defaultValues: { descripcion: '' }, // la gravedad arranca sin elegir
  });

  async function enviar(datos: DatosNovedad) {
    setErrorServidor(null);
    try {
      await registrarNovedad(identificacion, { descripcion: datos.descripcion, gravedad: datos.gravedad });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Novedad registrada');
    alRegistrar();
  }

  const prefijo = `novedad-${identificacion}`;

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label={`Registrar novedad de ${identificacion}`}>
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <CampoAreaTexto
        id={`${prefijo}-descripcion`}
        etiqueta="Descripción"
        rows={3}
        maxLength={MAXIMO_DESCRIPCION}
        placeholder="Qué se encontró en el apartamento"
        error={errors.descripcion?.message}
        {...register('descripcion')}
      />
      <div className="row align-items-end">
        <div className="col-12 col-md-5">
          <CampoSelect id={`${prefijo}-gravedad`} etiqueta="Gravedad" error={errors.gravedad?.message} {...register('gravedad')}>
            <option value="">Elija la gravedad</option>
            {GRAVEDADES.map((gravedad) => (
              <option key={gravedad} value={gravedad}>{ETIQUETA_GRAVEDAD[gravedad]}</option>
            ))}
          </CampoSelect>
        </div>
        {/*
          Paso M3 (L-21): en esta columna va el botón "Sugerir gravedad con IA". Leerá la descripción con getValues('descripcion')
          y, si la IA responde, propondrá la gravedad con setValue('gravedad', sugerida, { shouldValidate: true }). No es
          bloqueante: si la IA falla, el formulario sigue funcionando y la persona elige la gravedad a mano.
        */}
        <div className="col-12 col-md-7" />
      </div>
      <div className="d-flex gap-2">
        <button type="submit" className="btn btn-primary btn-sm" disabled={isSubmitting}>
          {isSubmitting ? 'Registrando…' : 'Registrar novedad'}
        </button>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={alCancelar} disabled={isSubmitting}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
