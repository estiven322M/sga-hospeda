import type { ComponentProps } from 'react';

interface CampoCasillaProps extends ComponentProps<'input'> {
  etiqueta: string;
}

/** Casilla (checkbox) con su etiqueta, con las clases de Bootstrap. Acepta todo lo de un <input>, incluido {...register()}. */
export function CampoCasilla({ etiqueta, id, className = '', ...propsDelInput }: CampoCasillaProps) {
  return (
    <div className="form-check">
      <input id={id} type="checkbox" className={`form-check-input ${className}`} {...propsDelInput} />
      <label className="form-check-label" htmlFor={id}>{etiqueta}</label>
    </div>
  );
}
