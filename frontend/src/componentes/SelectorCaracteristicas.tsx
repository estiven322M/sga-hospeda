import { CampoCasilla } from './CampoCasilla';
import { useCaracteristicas } from '../hooks/useCaracteristicas';
import type { Caracteristica } from '../modelos/apartamento';
import { etiquetarCaracteristica } from '../utilidades/etiquetas';

interface SelectorCaracteristicasProps {
  /** Prefijo de los id de los checkboxes (varios selectores en una misma página no deben repetirlos). */
  prefijo: string;
  seleccionadas: readonly Caracteristica[];
  alCambiar: (nuevas: Caracteristica[]) => void;
}

/**
 * Checkboxes con el catálogo de características que responde GET /caracteristicas (L-06): lo que se pinta es lo que
 * el backend ofrece. Es un componente controlado: sirve igual para filtrar un listado y para un formulario.
 */
export function SelectorCaracteristicas({ prefijo, seleccionadas, alCambiar }: SelectorCaracteristicasProps) {
  const { catalogo, error } = useCaracteristicas();

  function alternar(caracteristica: Caracteristica) {
    alCambiar(
      seleccionadas.includes(caracteristica)
        ? seleccionadas.filter((actual) => actual !== caracteristica)
        : [...seleccionadas, caracteristica],
    );
  }

  return (
    <fieldset className="mb-3">
      <legend className="form-label fs-6">Características</legend>
      {error && <div className="text-danger small">{error}</div>}
      {!catalogo && !error && <div className="text-body-secondary small">Cargando características…</div>}
      {catalogo?.map((caracteristica) => (
        <CampoCasilla
          key={caracteristica}
          id={`${prefijo}-${caracteristica}`}
          etiqueta={etiquetarCaracteristica(caracteristica)}
          checked={seleccionadas.includes(caracteristica)}
          onChange={() => alternar(caracteristica)}
        />
      ))}
    </fieldset>
  );
}
