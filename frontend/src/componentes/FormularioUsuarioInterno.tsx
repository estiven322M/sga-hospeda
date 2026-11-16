import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaUsuarioInterno, type FormularioUsuarioInterno } from '../esquemas/usuarios';
import { ROLES_INTERNOS } from '../modelos/usuario';
import { crearUsuarioInterno } from '../servicios/usuarioServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { CampoSelect } from './CampoSelect';

const CAMPOS = ['nombre', 'correo', 'contrasena', 'telefono', 'documento', 'fechaNacimiento', 'rol'] as const;

const ETIQUETA_ROL = {
  RECEPCIONISTA: 'Recepcionista',
  ADMINISTRADOR: 'Administrador',
  PERSONAL_SERVICIO: 'Personal de servicio',
} as const;

interface FormularioUsuarioInternoProps {
  alCrear: () => void;
  alCancelar: () => void;
}

/**
 * Crear un usuario INTERNO con su rol (7.2): solo un administrador asigna los roles internos; los huéspedes se registran solos.
 * El correo y el documento deben ser únicos: si ya existen, el backend responde 409 y se muestra su mensaje.
 */
export function FormularioUsuarioNuevo({ alCrear, alCancelar }: FormularioUsuarioInternoProps) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioUsuarioInterno>({
    resolver: zodResolver(esquemaUsuarioInterno),
    mode: 'onChange',
    defaultValues: { nombre: '', correo: '', contrasena: '', telefono: '', documento: '', fechaNacimiento: '', rol: 'RECEPCIONISTA' },
  });

  async function enviar(datos: FormularioUsuarioInterno) {
    setErrorServidor(null);
    try {
      await crearUsuarioInterno(datos);
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Usuario creado');
    alCrear();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Nuevo usuario interno" className="card card-body mb-4">
      <h2 className="h5">Nuevo usuario interno</h2>
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <div className="row">
        <div className="col-12 col-md-6">
          <Campo id="usuario-nombre" etiqueta="Nombre" autoComplete="off" error={errors.nombre?.message} {...register('nombre')} />
        </div>
        <div className="col-12 col-md-6">
          <Campo id="usuario-correo" type="email" etiqueta="Correo electrónico" autoComplete="off" error={errors.correo?.message} {...register('correo')} />
        </div>
        <div className="col-12 col-md-6">
          <Campo id="usuario-documento" etiqueta="Documento" autoComplete="off" error={errors.documento?.message} {...register('documento')} />
        </div>
        <div className="col-12 col-md-6">
          <Campo id="usuario-telefono" type="tel" etiqueta="Teléfono" autoComplete="off" error={errors.telefono?.message} {...register('telefono')} />
        </div>
        <div className="col-12 col-md-4">
          <Campo id="usuario-nacimiento" type="date" etiqueta="Fecha de nacimiento" error={errors.fechaNacimiento?.message} {...register('fechaNacimiento')} />
        </div>
        <div className="col-12 col-md-4">
          <CampoSelect id="usuario-rol" etiqueta="Rol" error={errors.rol?.message} {...register('rol')}>
            {ROLES_INTERNOS.map((rol) => (
              <option key={rol} value={rol}>{ETIQUETA_ROL[rol]}</option>
            ))}
          </CampoSelect>
        </div>
        <div className="col-12 col-md-4">
          <Campo id="usuario-contrasena" type="password" etiqueta="Contraseña inicial" autoComplete="new-password" error={errors.contrasena?.message} {...register('contrasena')} />
        </div>
      </div>
      <div className="d-flex gap-2">
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
          {isSubmitting ? 'Creando…' : 'Crear usuario'}
        </button>
        <button type="button" className="btn btn-outline-secondary" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
