import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaPago, type FormularioPago } from '../esquemas/reserva';
import { useAlojamiento } from '../hooks/useAlojamiento';
import { registrarPago } from '../servicios/folioServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { hoyISO } from '../utilidades/fechas';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { CampoSelect } from './CampoSelect';

const CAMPOS = ['medio', 'valor', 'fecha'] as const;

/**
 * Registrar un pago en el folio (CU-08): medio, valor en pesos y fecha. El medio se elige entre los que el alojamiento
 * acepta (L-18, GET /alojamiento); el backend rechaza con 409 uno que no lo sea. Si la lista no carga, se puede escribir
 * el medio y el backend decide. Los pagos no se editan ni se borran.
 */
export function FormularioPago({ codigo, alRegistrar }: { codigo: string; alRegistrar: () => void }) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const { alojamiento, error: errorAlojamiento } = useAlojamiento();
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioPago>({
    resolver: zodResolver(esquemaPago),
    mode: 'onChange',
    defaultValues: { medio: '', valor: '', fecha: hoyISO() },
  });

  async function enviar(datos: FormularioPago) {
    setErrorServidor(null);
    try {
      await registrarPago(codigo, { medio: datos.medio, valor: Number(datos.valor), fecha: datos.fecha });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito('Pago registrado');
    reset({ medio: '', valor: '', fecha: hoyISO() });
    alRegistrar();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Registrar pago">
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <div className="row">
        <div className="col-12 col-md-4">
          {errorAlojamiento ? (
            // La lista no cargó: se puede escribir el medio y el backend decide (409 si no es aceptado)
            <Campo id="pago-medio" etiqueta="Medio de pago" error={errors.medio?.message} {...register('medio')} />
          ) : (
            <CampoSelect id="pago-medio" etiqueta="Medio de pago" error={errors.medio?.message} {...register('medio')}>
              <option value="">{alojamiento ? 'Elija un medio' : 'Cargando medios…'}</option>
              {alojamiento?.mediosPago.map((medio) => (
                <option key={medio} value={medio}>{medio}</option>
              ))}
            </CampoSelect>
          )}
        </div>
        <div className="col-12 col-md-4">
          <Campo id="pago-valor" type="number" min={1} etiqueta="Valor (pesos)" error={errors.valor?.message} {...register('valor')} />
        </div>
        <div className="col-12 col-md-4">
          <Campo id="pago-fecha" type="date" etiqueta="Fecha" error={errors.fecha?.message} {...register('fecha')} />
        </div>
      </div>
      <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
        {isSubmitting ? 'Registrando…' : 'Registrar pago'}
      </button>
    </form>
  );
}
