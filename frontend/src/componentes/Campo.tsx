import type { ComponentProps } from 'react';

// Recibe todo lo que acepta un <input> (type, placeholder, onChange, ref...) más dos props propias.
interface CampoProps extends ComponentProps<'input'> {
  etiqueta: string;
  error?: string;
}

/**
 * Etiqueta + input + mensaje de error con las clases de Bootstrap. En React 19 `ref` es una prop más, así que
 * {...register('campo')} de React Hook Form funciona directamente, sin forwardRef.
 */
export function Campo({ etiqueta, error, id, className = '', ...propsDelInput }: CampoProps) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label">{etiqueta}</label>
      <input
        id={id}
        className={`form-control ${error ? 'is-invalid' : ''} ${className}`}
        aria-invalid={error ? true : undefined}
        {...propsDelInput}
      />
      <div className="invalid-feedback">{error}</div>
    </div>
  );
}
