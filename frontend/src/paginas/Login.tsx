import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';
import { Campo } from '../componentes/Campo';
import { esquemaLogin, type FormularioLogin } from '../esquemas/login';
import { useAuth } from '../hooks/useAuth';
import { RUTAS } from '../rutas/rutas';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';

export default function Login() {
  const { iniciarSesion } = useAuth();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioLogin>({ resolver: zodResolver(esquemaLogin) });

  async function enviar(datos: FormularioLogin) {
    setErrorServidor(null);
    try {
      // Si sale bien, la sesión cambia y RutaPublica lleva a la página inicial del rol: no hay que navegar aquí.
      await iniciarSesion(datos);
    } catch (error) {
      // 401: el backend responde un mensaje genérico que no revela si el correo existe. Se muestra tal cual.
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if (campo === 'correo' || campo === 'contrasena') setError(campo, { message: mensaje });
      }
    }
  }

  return (
    <section className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-5">
        <h1 className="h3 mb-3">Ingresar</h1>

        {errorServidor && (
          <div className="alert alert-danger" role="alert">
            {errorServidor}
          </div>
        )}

        <form onSubmit={handleSubmit(enviar)} noValidate>
          <Campo
            id="correo"
            type="email"
            etiqueta="Correo electrónico"
            autoComplete="username"
            error={errors.correo?.message}
            {...register('correo')}
          />
          <Campo
            id="contrasena"
            type="password"
            etiqueta="Contraseña"
            autoComplete="current-password"
            error={errors.contrasena?.message}
            {...register('contrasena')}
          />
          <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
            {isSubmitting ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>

        <p className="mt-3 mb-1 text-center">
          <Link to={RUTAS.recuperarContrasena}>¿Olvidó su contraseña?</Link>
        </p>
        <p className="text-center">
          ¿Aún no tiene cuenta? <Link to={RUTAS.registro}>Regístrese</Link>
        </p>
      </div>
    </section>
  );
}
