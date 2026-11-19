import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { Campo } from '../componentes/Campo';
import { PanelAcceso } from '../componentes/PanelAcceso';
import {
  esquemaConfirmarRecuperacion,
  esquemaSolicitarRecuperacion,
  type FormularioConfirmarRecuperacion,
  type FormularioSolicitarRecuperacion,
} from '../esquemas/usuarios';
import { RUTAS } from '../rutas/rutas';
import { confirmarRecuperacion, solicitarRecuperacion } from '../servicios/authServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';

/** Paso 1: se pide el código al correo. La respuesta es siempre la misma, esté o no registrado el correo. */
function PasoSolicitar({ alPedirCodigo }: { alPedirCodigo: (correo: string, mensaje: string) => void }) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormularioSolicitarRecuperacion>({ resolver: zodResolver(esquemaSolicitarRecuperacion) });

  async function enviar(datos: FormularioSolicitarRecuperacion) {
    setErrorServidor(null);
    try {
      const respuesta = await solicitarRecuperacion({ correo: datos.correo });
      alPedirCodigo(datos.correo, respuesta.mensaje);
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
    }
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Solicitar código de recuperación">
      <p className="text-body-secondary">Escriba el correo de su cuenta y le enviaremos un código de 6 dígitos.</p>
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <Campo id="recuperar-correo" type="email" etiqueta="Correo electrónico" autoComplete="username" error={errors.correo?.message} {...register('correo')} />
      <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
        {isSubmitting ? 'Enviando…' : 'Enviar código'}
      </button>
    </form>
  );
}

interface PasoConfirmarProps {
  correoInicial: string;
  aviso: string | null;
  alPedirOtroCodigo: () => void;
}

/** Paso 2: el código recibido y la contraseña nueva. Cualquier problema con el código es el mismo 409: no se revela el motivo. */
function PasoConfirmar({ correoInicial, aviso, alPedirOtroCodigo }: PasoConfirmarProps) {
  const navigate = useNavigate();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioConfirmarRecuperacion>({
    resolver: zodResolver(esquemaConfirmarRecuperacion),
    mode: 'onChange',
    defaultValues: { correo: correoInicial, codigo: '', contrasenaNueva: '', confirmarContrasena: '' },
  });

  async function enviar(datos: FormularioConfirmarRecuperacion) {
    setErrorServidor(null);
    try {
      await confirmarRecuperacion({ correo: datos.correo, codigo: datos.codigo, contrasenaNueva: datos.contrasenaNueva });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if (campo === 'correo' || campo === 'codigo' || campo === 'contrasenaNueva') setError(campo, { message: mensaje });
      }
      return;
    }
    notificarExito('Contraseña cambiada: ya puede iniciar sesión');
    void navigate(RUTAS.login);
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Confirmar la recuperación">
      {aviso && <div className="alert alert-info" role="status">{aviso}</div>}
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <Campo id="confirmar-correo" type="email" etiqueta="Correo electrónico" autoComplete="username" error={errors.correo?.message} {...register('correo')} />
      <Campo
        id="confirmar-codigo"
        etiqueta="Código de 6 dígitos"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        error={errors.codigo?.message}
        {...register('codigo')}
      />
      <Campo
        id="confirmar-contrasena"
        type="password"
        etiqueta="Contraseña nueva"
        autoComplete="new-password"
        error={errors.contrasenaNueva?.message}
        {...register('contrasenaNueva', { deps: ['confirmarContrasena'] })}
      />
      <div className="form-text mb-3">Mínimo 8 caracteres, con al menos una mayúscula y un número.</div>
      <Campo
        id="confirmar-repetir"
        type="password"
        etiqueta="Confirmar la contraseña nueva"
        autoComplete="new-password"
        error={errors.confirmarContrasena?.message}
        {...register('confirmarContrasena')}
      />
      <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
        {isSubmitting ? 'Cambiando…' : 'Cambiar contraseña'}
      </button>
      <p className="text-center mt-3 mb-0">
        <button type="button" className="btn btn-link p-0" onClick={alPedirOtroCodigo}>Pedir un código nuevo</button>
      </p>
    </form>
  );
}

/**
 * Recuperar la contraseña (7.2), pública: primero se pide el código al correo y luego se confirma con él y la contraseña nueva.
 * El código vale 15 minutos y sirve una sola vez. Quien ya tiene un código puede pasar directo al segundo paso.
 */
export default function RecuperarContrasena() {
  const [paso, setPaso] = useState<'solicitar' | 'confirmar'>('solicitar');
  const [correo, setCorreo] = useState('');
  const [aviso, setAviso] = useState<string | null>(null);

  return (
    <PanelAcceso titulo="Recuperar la contraseña" subtitulo="Le enviaremos un código a su correo para crear una contraseña nueva.">

        {paso === 'solicitar' ? (
          <>
            <PasoSolicitar
              alPedirCodigo={(correoPedido, mensaje) => {
                setCorreo(correoPedido);
                setAviso(mensaje);
                setPaso('confirmar');
              }}
            />
            <p className="text-center mt-3 mb-1">
              <button type="button" className="btn btn-link p-0" onClick={() => { setAviso(null); setPaso('confirmar'); }}>
                Ya tengo un código
              </button>
            </p>
          </>
        ) : (
          <PasoConfirmar correoInicial={correo} aviso={aviso} alPedirOtroCodigo={() => setPaso('solicitar')} />
        )}

        <p className="mt-3 text-center">
          <Link to={RUTAS.login}>Volver a ingresar</Link>
        </p>
    </PanelAcceso>
  );
}
