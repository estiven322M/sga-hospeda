import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaPerfil, type FormularioPerfil } from '../esquemas/usuarios';
import type { UsuarioResponse } from '../modelos/usuario';
import { actualizarMiPerfil } from '../servicios/usuarioServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';

const CAMPOS = ['nombre', 'telefono'] as const;

interface EdicionPerfilProps {
  perfil: UsuarioResponse;
  alActualizar: (perfil: UsuarioResponse) => void;
}

/**
 * Editar el perfil (7.2): solo el nombre y el teléfono. El correo, el documento y el rol se muestran pero no se editan
 * (el backend los ignora aunque se envíen). El perfil es siempre el de quien tiene la sesión: lo decide el token.
 */
export function EdicionPerfil({ perfil, alActualizar }: EdicionPerfilProps) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<FormularioPerfil>({
    resolver: zodResolver(esquemaPerfil),
    mode: 'onChange',
    defaultValues: { nombre: perfil.nombre, telefono: perfil.telefono },
  });

  async function enviar(datos: FormularioPerfil) {
    setErrorServidor(null);
    try {
      alActualizar(await actualizarMiPerfil({ nombre: datos.nombre, telefono: datos.telefono }));
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Perfil actualizado');
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Datos del perfil">
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <dl className="row small mb-3">
        <dt className="col-sm-4">Correo electrónico</dt>
        <dd className="col-sm-8">{perfil.correo} <span className="text-body-secondary">(no se puede cambiar)</span></dd>
        <dt className="col-sm-4">Documento</dt>
        <dd className="col-sm-8">{perfil.documento}</dd>
        <dt className="col-sm-4">Fecha de nacimiento</dt>
        <dd className="col-sm-8">{perfil.fechaNacimiento}</dd>
        <dt className="col-sm-4">Rol</dt>
        <dd className="col-sm-8 mb-0">{perfil.rol}</dd>
      </dl>
      <Campo id="perfil-nombre" etiqueta="Nombre" autoComplete="name" error={errors.nombre?.message} {...register('nombre')} />
      <Campo id="perfil-telefono" type="tel" etiqueta="Teléfono" autoComplete="tel" error={errors.telefono?.message} {...register('telefono')} />
      <button type="submit" className="btn btn-primary" disabled={isSubmitting || !isDirty}>
        {isSubmitting ? 'Guardando…' : 'Guardar cambios'}
      </button>
    </form>
  );
}
