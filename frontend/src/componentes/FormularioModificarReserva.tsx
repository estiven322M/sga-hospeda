import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { useAutor } from '../hooks/useAutor';
import { useApartamentos } from '../hooks/useApartamentos';
import { esquemaModificarReserva, type FormularioModificarReserva } from '../esquemas/reserva';
import type { AjusteFolioResponse } from '../modelos/folio';
import type { ReservaDetalleResponse } from '../modelos/reserva';
import { obtenerFolio } from '../servicios/folioServicio';
import { modificarReserva } from '../servicios/reservaServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { CamposOcupantes } from './CamposOcupantes';
import { Campo } from './Campo';
import { CampoSelect } from './CampoSelect';

interface FormularioModificarReservaProps {
  reserva: ReservaDetalleResponse;
  /** Cuántos ajustes tenía el folio antes de modificar: lo que aparezca después es el ajuste de ESTA modificación. */
  ajustesAntes: number | null;
  /** Se llama tras modificar, con los ajustes nuevos del folio (vacío si no se pudieron leer). */
  alModificar: (ajustesNuevos: AjusteFolioResponse[]) => void;
}

const CAMPOS = ['identificacionApartamento', 'fechaEntrada', 'fechaSalida'] as const;

/**
 * Modificar fechas, ocupantes y apartamento de una reserva (CU-04). El backend repite las validaciones de la creación,
 * recalcula el valor con las tarifas vigentes y registra la diferencia como ajuste en el folio: aquí solo se envía y,
 * después, se lee el folio para mostrar ese ajuste. Un 409 (RP-01, RP-03, disponibilidad...) se muestra tal cual.
 */
export function FormularioModificarReserva({ reserva, ajustesAntes, alModificar }: FormularioModificarReservaProps) {
  const autor = useAutor();
  const { apartamentos } = useApartamentos();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  const formulario = useForm<FormularioModificarReserva>({
    resolver: zodResolver(esquemaModificarReserva),
    mode: 'onChange',
    defaultValues: {
      identificacionApartamento: reserva.identificacionApartamento,
      fechaEntrada: reserva.fechaEntrada,
      fechaSalida: reserva.fechaSalida,
      // El titular va primero: CamposOcupantes rotula así a la primera fila y no deja quitarla
      ocupantes: [...reserva.ocupantes]
        .sort((a, b) => Number(b.documento === reserva.titular.documento) - Number(a.documento === reserva.titular.documento))
        .map(({ documento, nombre, fechaNacimiento }) => ({ documento, nombre, fechaNacimiento })),
    },
  });
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = formulario;

  async function enviar(datos: FormularioModificarReserva) {
    setErrorServidor(null);
    try {
      await modificarReserva(reserva.codigo, {
        // Sin identificacionApartamento la reserva conserva el apartamento actual
        ...(datos.identificacionApartamento !== reserva.identificacionApartamento && {
          identificacionApartamento: datos.identificacionApartamento,
        }),
        fechaEntrada: datos.fechaEntrada,
        fechaSalida: datos.fechaSalida,
        ocupantes: datos.ocupantes,
        autor,
      });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    // La reserva YA cambió: leer el folio para mostrar el ajuste no puede hacerse pasar por un fallo de la modificación.
    let ajustesNuevos: AjusteFolioResponse[] = [];
    try {
      const folio = await obtenerFolio(reserva.codigo);
      if (ajustesAntes !== null) ajustesNuevos = folio.ajustes.slice(ajustesAntes);
    } catch {
      // sin el folio no se puede mostrar el ajuste; la página lo recarga de todos modos
    }
    notificarExito('Reserva modificada');
    alModificar(ajustesNuevos);
  }

  return (
    <FormProvider {...formulario}>
      <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Modificar la reserva">
        {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
        <CampoSelect id="mod-apartamento" etiqueta="Apartamento" error={errors.identificacionApartamento?.message} {...register('identificacionApartamento')}>
          {apartamentos
            ?.filter((apartamento) => apartamento.activo || apartamento.identificacion === reserva.identificacionApartamento)
            .map((apartamento) => (
              <option key={apartamento.identificacion} value={apartamento.identificacion}>
                {apartamento.identificacion} · {apartamento.nombre} (capacidad {apartamento.capacidad})
              </option>
            ))}
          {/* Mientras carga el catálogo, el apartamento actual sigue siendo una opción válida */}
          {!apartamentos && <option value={reserva.identificacionApartamento}>{reserva.identificacionApartamento}</option>}
        </CampoSelect>
        <div className="row">
          <div className="col-12 col-md-6">
            <Campo id="mod-fechaEntrada" type="date" etiqueta="Entrada" error={errors.fechaEntrada?.message} {...register('fechaEntrada', { deps: ['fechaSalida'] })} />
          </div>
          <div className="col-12 col-md-6">
            <Campo id="mod-fechaSalida" type="date" etiqueta="Salida" error={errors.fechaSalida?.message} {...register('fechaSalida')} />
          </div>
        </div>
        <CamposOcupantes primerEsTitular />
        <button type="submit" className="btn btn-primary mt-2" disabled={isSubmitting}>
          {isSubmitting ? 'Modificando…' : 'Guardar cambios'}
        </button>
      </form>
    </FormProvider>
  );
}
