import type { ComponentProps } from 'react';

interface CampoSelectProps extends ComponentProps<'select'> {
  etiqueta: string;
  error?: string;
}

/** Etiqueta + <select> + mensaje de error (is-invalid / invalid-feedback), como Campo pero para listas desplegables. */
export function CampoSelect({ etiqueta, error, id, className = '', children, ...propsDelSelect }: CampoSelectProps) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label">{etiqueta}</label>
      <select
        id={id}
        className={`form-select ${error ? 'is-invalid' : ''} ${className}`}
        aria-invalid={error ? true : undefined}
        {...propsDelSelect}
      >
        {children}
      </select>
      <div className="invalid-feedback">{error}</div>
    </div>
  );
}
