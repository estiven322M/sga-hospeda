import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaAutorizacionCierre, type FormularioAutorizacionCierre } from '../esquemas/reserva';
import { useAuth } from '../hooks/useAuth';
import { useAutor } from '../hooks/useAutor';
import type { FolioResponse } from '../modelos/folio';
import { cerrarFolio } from '../servicios/folioServicio';
import { mensajeDeError } from '../utilidades/errores';
import { formatearPesos } from '../utilidades/formato';
import { confirmarAccion, notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';

interface CierreFolioProps {
  codigo: string;
  folio: FolioResponse;
  alCerrar: () => void;
}

/**
 * Cerrar el folio (CU-11). Sin saldo, cualquiera de recepción lo cierra. Con saldo pendiente solo el ADMINISTRADOR
 * puede cerrarlo, con una autorización explícita (quién autoriza y por qué) que queda registrada; para los demás roles
 * esa opción no se ofrece (si la pidieran, el backend responde 403: la seguridad real es suya).
 */
export function CierreFolio({ codigo, folio, alCerrar }: CierreFolioProps) {
  const { tieneRol } = useAuth();
  const autor = useAutor();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [cerrando, setCerrando] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormularioAutorizacionCierre>({
    resolver: zodResolver(esquemaAutorizacionCierre),
    defaultValues: { autorizadoPor: autor, motivo: '' },
  });

  if (folio.cerrado) return null;

  const conSaldo = folio.saldo !== 0;

  async function ejecutar(solicitud: { autorizadoPor?: string; motivo?: string }) {
    setErrorServidor(null);
    setCerrando(true);
    try {
      await cerrarFolio(codigo, solicitud);
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      return;
    } finally {
      setCerrando(false);
    }
    notificarExito('Folio cerrado');
    alCerrar();
  }

  async function cerrarSinSaldo() {
    const aceptada = await confirmarAccion('Cerrar el folio', 'Un folio cerrado no admite más movimientos. No se puede deshacer.');
    if (aceptada) await ejecutar({});
  }

  async function cerrarConAutorizacion(datos: FormularioAutorizacionCierre) {
    const aceptada = await confirmarAccion(
      'Cerrar el folio con saldo pendiente',
      `El folio tiene un saldo de ${formatearPesos(folio.saldo)}. Se cerrará con su autorización y quedará registrado quién autoriza y el motivo. No se puede deshacer.`,
    );
    if (aceptada) await ejecutar({ autorizadoPor: datos.autorizadoPor, motivo: datos.motivo });
  }

  if (conSaldo && !tieneRol('ADMINISTRADOR')) {
    return (
      <p className="small text-body-secondary">
        El folio tiene un saldo de {formatearPesos(folio.saldo)}: para cerrarlo con saldo se necesita la autorización del administrador.
      </p>
    );
  }

  return (
    <div>
      {errorServidor && <div className="alert alert-warning" role="alert">{errorServidor}</div>}
      {conSaldo ? (
        <form onSubmit={handleSubmit(cerrarConAutorizacion)} noValidate aria-label="Cerrar el folio con saldo">
          <p className="small text-body-secondary">
            El folio tiene un saldo de {formatearPesos(folio.saldo)}. Como administrador puede cerrarlo con autorización.
          </p>
          <Campo id="cierre-autorizadoPor" etiqueta="Autorizado por" error={errors.autorizadoPor?.message} {...register('autorizadoPor')} />
          <Campo id="cierre-motivo" etiqueta="Motivo" error={errors.motivo?.message} {...register('motivo')} />
          <button type="submit" className="btn btn-danger" disabled={cerrando}>
            {cerrando ? 'Cerrando…' : 'Cerrar folio con autorización'}
          </button>
        </form>
      ) : (
        <button type="button" className="btn btn-primary" onClick={() => void cerrarSinSaldo()} disabled={cerrando}>
          {cerrando ? 'Cerrando…' : 'Cerrar folio'}
        </button>
      )}
    </div>
  );
}
