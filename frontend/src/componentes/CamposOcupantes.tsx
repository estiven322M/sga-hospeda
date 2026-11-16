import { useFieldArray, useFormContext } from 'react-hook-form';
import { OCUPANTE_VACIO, type FormularioOcupante } from '../esquemas/ocupante';
import { Campo } from './Campo';

interface ConOcupantes {
  ocupantes: FormularioOcupante[];
}

interface CamposOcupantesProps {
  /** El primer ocupante es el titular y su documento no se puede cambiar (debe ser el de la sesión). */
  documentoTitularFijo?: boolean;
  /** El nombre del titular viene de la sesión y no se puede cambiar. */
  nombreTitularFijo?: boolean;
}

/**
 * Lista dinámica de ocupantes (documento, nombre y fecha de nacimiento) con useFieldArray. Se usa dentro de un
 * <FormProvider>: así sirve igual en la búsqueda y en la reserva, y no depende del tipo del formulario que la contiene.
 * La fecha de nacimiento es lo que permite al backend decidir quién paga y si hay adulto responsable.
 */
export function CamposOcupantes({ documentoTitularFijo = false, nombreTitularFijo = false }: CamposOcupantesProps) {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<ConOcupantes>();
  const { fields, append, remove } = useFieldArray({ control, name: 'ocupantes' });

  return (
    <fieldset>
      <legend className="h6">Ocupantes</legend>
      {fields.map((campo, indice) => {
        const errorDelOcupante = errors.ocupantes?.[indice];
        return (
          // campo.id lo genera React Hook Form: es estable aunque se quite un ocupante del medio (el índice no lo es)
          <div key={campo.id} className="border rounded p-2 mb-2">
            <div className="d-flex justify-content-between align-items-center mb-1">
              <span className="small fw-semibold">{indice === 0 && documentoTitularFijo ? 'Titular (usted)' : `Ocupante ${indice + 1}`}</span>
              {fields.length > 1 && !(indice === 0 && documentoTitularFijo) && (
                <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => remove(indice)}>
                  Quitar
                </button>
              )}
            </div>
            <Campo
              id={`ocupante-${indice}-documento`}
              etiqueta="Documento"
              readOnly={indice === 0 && documentoTitularFijo}
              error={errorDelOcupante?.documento?.message}
              {...register(`ocupantes.${indice}.documento`)}
            />
            <Campo
              id={`ocupante-${indice}-nombre`}
              etiqueta="Nombre"
              readOnly={indice === 0 && nombreTitularFijo}
              error={errorDelOcupante?.nombre?.message}
              {...register(`ocupantes.${indice}.nombre`)}
            />
            <Campo
              id={`ocupante-${indice}-fechaNacimiento`}
              type="date"
              etiqueta="Fecha de nacimiento"
              error={errorDelOcupante?.fechaNacimiento?.message}
              {...register(`ocupantes.${indice}.fechaNacimiento`)}
            />
          </div>
        );
      })}
      {errors.ocupantes?.message && <div className="text-danger small mb-2">{errors.ocupantes.message}</div>}
      <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => append({ ...OCUPANTE_VACIO })}>
        Agregar ocupante
      </button>
    </fieldset>
  );
}
