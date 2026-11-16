import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaCargoServicio, type FormularioCargoServicio } from '../esquemas/administracion';
import { useConsulta } from '../hooks/useConsulta';
import { registrarCargoServicio } from '../servicios/folioServicio';
import { listarTodosLosServiciosActivos } from '../servicios/servicioAdicionalServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { formatearPesos } from '../utilidades/formato';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { CampoSelect } from './CampoSelect';

const CAMPOS = ['codigoServicio', 'cantidad'] as const;

/**
 * Registrar un cargo de servicio adicional en el folio (L-17, 7.7): se elige un servicio activo que genere cargo y una
 * cantidad; el valor del cargo (valor del servicio × cantidad) lo calcula el backend. Las reglas (reserva CONFIRMADA o
 * EN_CURSO, servicio activo, folio abierto) las decide el dominio y su 409 se muestra tal cual.
 */
export function FormularioCargoServicioNuevo({ codigo, alRegistrar }: { codigo: string; alRegistrar: () => void }) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const { resultado } = useConsulta('servicios-activos', (signal) => listarTodosLosServiciosActivos(signal));
  const servicios = resultado?.tipo === 'datos' ? resultado.datos.filter((servicio) => servicio.generaCargo) : null;

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioCargoServicio>({
    resolver: zodResolver(esquemaCargoServicio),
    mode: 'onChange',
    defaultValues: { codigoServicio: '', cantidad: '1' },
  });

  async function enviar(datos: FormularioCargoServicio) {
    setErrorServidor(null);
    try {
      await registrarCargoServicio(codigo, { codigoServicio: datos.codigoServicio, cantidad: Number(datos.cantidad) });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Servicio adicional cargado al folio');
    reset({ codigoServicio: '', cantidad: '1' });
    alRegistrar();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Registrar cargo de servicio adicional">
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      {resultado?.tipo === 'error' && <div className="alert alert-warning small" role="alert">{resultado.mensaje}</div>}
      <div className="row">
        <div className="col-12 col-md-8">
          <CampoSelect id="cargo-servicio" etiqueta="Servicio" error={errors.codigoServicio?.message} {...register('codigoServicio')}>
            <option value="">{servicios ? 'Elija un servicio' : 'Cargando servicios…'}</option>
            {servicios?.map((servicio) => (
              <option key={servicio.codigo} value={servicio.codigo}>
                {servicio.nombre} · {formatearPesos(servicio.valor)}
              </option>
            ))}
          </CampoSelect>
        </div>
        <div className="col-12 col-md-4">
          <Campo id="cargo-cantidad" type="number" min={1} max={100} etiqueta="Cantidad" error={errors.cantidad?.message} {...register('cantidad')} />
        </div>
      </div>
      <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
        {isSubmitting ? 'Registrando…' : 'Registrar cargo'}
      </button>
    </form>
  );
}
