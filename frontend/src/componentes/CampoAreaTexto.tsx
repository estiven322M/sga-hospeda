import type { ComponentProps } from 'react';

interface CampoAreaTextoProps extends ComponentProps<'textarea'> {
  etiqueta: string;
  error?: string;
}

/** Etiqueta + <textarea> + mensaje de error (is-invalid / invalid-feedback), como Campo pero para textos largos. */
export function CampoAreaTexto({ etiqueta, error, id, className = '', ...propsDelArea }: CampoAreaTextoProps) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label">{etiqueta}</label>
      <textarea
        id={id}
        className={`form-control ${error ? 'is-invalid' : ''} ${className}`}
        aria-invalid={error ? true : undefined}
        {...propsDelArea}
      />
      <div className="invalid-feedback">{error}</div>
    </div>
  );
}
