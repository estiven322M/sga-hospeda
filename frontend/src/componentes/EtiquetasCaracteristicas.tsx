import { etiquetarCaracteristica } from '../utilidades/etiquetas';

/** Las características de un apartamento como etiquetas (badges) de Bootstrap. */
export function EtiquetasCaracteristicas({ caracteristicas }: { caracteristicas: readonly string[] }) {
  if (caracteristicas.length === 0) {
    return null;
  }
  return (
    <ul className="list-inline mb-0">
      {caracteristicas.map((caracteristica) => (
        <li key={caracteristica} className="list-inline-item">
          <span className="badge text-bg-light border">{etiquetarCaracteristica(caracteristica)}</span>
        </li>
      ))}
    </ul>
  );
}
