import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaCambioContrasena, type FormularioCambioContrasena } from '../esquemas/usuarios';
import { cambiarMiContrasena } from '../servicios/usuarioServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';

const CAMPOS = ['contrasenaActual', 'contrasenaNueva'] as const;

/**
 * Cambio voluntario de contraseña (7.2): con la contraseña actual y la nueva, que sigue las reglas del registro (8 caracteres,
 * una mayúscula y un número). Si la actual es incorrecta o la nueva es igual a la actual, el backend responde 409 y se muestra su
 * mensaje. La sesión sigue abierta: el token no cambia, solo la contraseña con la que se inicia la próxima sesión.
 */
export function CambioContrasena() {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioCambioContrasena>({
    resolver: zodResolver(esquemaCambioContrasena),
    mode: 'onChange',
    defaultValues: { contrasenaActual: '', contrasenaNueva: '', confirmarContrasena: '' },
  });

  async function enviar(datos: FormularioCambioContrasena) {
    setErrorServidor(null);
    try {
      await cambiarMiContrasena({ contrasenaActual: datos.contrasenaActual, contrasenaNueva: datos.contrasenaNueva });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Contraseña cambiada');
    reset();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Cambiar contraseña">
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <Campo
        id="contrasena-actual"
        type="password"
        etiqueta="Contraseña actual"
        autoComplete="current-password"
        error={errors.contrasenaActual?.message}
        {...register('contrasenaActual')}
      />
      <Campo
        id="contrasena-nueva"
        type="password"
        etiqueta="Contraseña nueva"
        autoComplete="new-password"
        error={errors.contrasenaNueva?.message}
        {...register('contrasenaNueva', { deps: ['confirmarContrasena'] })}
      />
      <div className="form-text mb-3">Mínimo 8 caracteres, con al menos una mayúscula y un número.</div>
      <Campo
        id="contrasena-confirmar"
        type="password"
        etiqueta="Confirmar la contraseña nueva"
        autoComplete="new-password"
        error={errors.confirmarContrasena?.message}
        {...register('confirmarContrasena')}
      />
      <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
        {isSubmitting ? 'Cambiando…' : 'Cambiar contraseña'}
      </button>
    </form>
  );
}
