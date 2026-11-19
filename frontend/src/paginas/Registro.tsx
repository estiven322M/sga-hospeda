import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { Campo } from '../componentes/Campo';
import { PanelAcceso } from '../componentes/PanelAcceso';
import { esquemaRegistro, type FormularioRegistro } from '../esquemas/registro';
import { useAuth } from '../hooks/useAuth';
import type { RegistroUsuarioRequest } from '../modelos/auth';
import { RUTAS } from '../rutas/rutas';
import { registrar } from '../servicios/authServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';

const CAMPOS_DEL_FORMULARIO = ['nombre', 'correo', 'contrasena', 'telefono', 'documento', 'fechaNacimiento'] as const;

/** Registro público: siempre crea un HUESPED. */
export default function Registro() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioRegistro>({ resolver: zodResolver(esquemaRegistro) });

  async function enviar(datos: FormularioRegistro) {
    setErrorServidor(null);
    // La confirmación solo existe en el formulario: no es parte de RegistroUsuarioRequest.
    const solicitud: RegistroUsuarioRequest = {
      nombre: datos.nombre,
      correo: datos.correo,
      contrasena: datos.contrasena,
      telefono: datos.telefono,
      documento: datos.documento,
      fechaNacimiento: datos.fechaNacimiento,
    };
    try {
      await registrar(solicitud);
    } catch (error) {
      // 409 (correo o documento ya registrados): el mensaje del backend. 400: un error por campo.
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS_DEL_FORMULARIO as readonly string[]).includes(campo)) {
          setError(campo as (typeof CAMPOS_DEL_FORMULARIO)[number], { message: mensaje });
        }
      }
      return;
    }

    notificarExito('Cuenta creada. Bienvenido a Hospeda.');
    try {
      // Con la cuenta recién creada se ingresa directo; RutaPublica lleva a la página inicial del huésped.
      await iniciarSesion({ correo: datos.correo, contrasena: datos.contrasena });
    } catch {
      navigate(RUTAS.login, { replace: true });
    }
  }

  return (
    <PanelAcceso titulo="Crear cuenta" subtitulo="Regístrese como huésped para reservar en línea.">

        {errorServidor && (
          <div className="alert alert-danger" role="alert">
            {errorServidor}
          </div>
        )}

        <form onSubmit={handleSubmit(enviar)} noValidate>
          <Campo id="nombre" etiqueta="Nombre completo" autoComplete="name" error={errors.nombre?.message} {...register('nombre')} />
          <Campo
            id="correo"
            type="email"
            etiqueta="Correo electrónico"
            autoComplete="email"
            error={errors.correo?.message}
            {...register('correo')}
          />
          <div className="row">
            <div className="col-12 col-md-6">
              <Campo
                id="contrasena"
                type="password"
                etiqueta="Contraseña"
                autoComplete="new-password"
                error={errors.contrasena?.message}
                {...register('contrasena')}
              />
            </div>
            <div className="col-12 col-md-6">
              <Campo
                id="confirmarContrasena"
                type="password"
                etiqueta="Confirmar contraseña"
                autoComplete="new-password"
                error={errors.confirmarContrasena?.message}
                {...register('confirmarContrasena')}
              />
            </div>
          </div>
          <div className="form-text mb-3">Mínimo 8 caracteres, con una mayúscula y un número.</div>
          <Campo
            id="telefono"
            type="tel"
            etiqueta="Teléfono"
            autoComplete="tel"
            error={errors.telefono?.message}
            {...register('telefono')}
          />
          <div className="row">
            <div className="col-12 col-md-6">
              <Campo id="documento" etiqueta="Documento de identidad" error={errors.documento?.message} {...register('documento')} />
            </div>
            <div className="col-12 col-md-6">
              <Campo
                id="fechaNacimiento"
                type="date"
                etiqueta="Fecha de nacimiento"
                autoComplete="bday"
                error={errors.fechaNacimiento?.message}
                {...register('fechaNacimiento')}
              />
            </div>
          </div>
          <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
            {isSubmitting ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className="mt-3 text-center">
          ¿Ya tiene cuenta? <Link to={RUTAS.login}>Ingrese</Link>
        </p>
    </PanelAcceso>
  );
}
