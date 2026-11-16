interface PaginacionProps {
  /** Página actual, desde 0, como en el backend (`pagina` de PaginaResponse). */
  paginaActual: number;
  totalPaginas: number;
  alCambiar: (pagina: number) => void;
}

const MAXIMO_VISIBLES = 5;

/**
 * Paginación de Bootstrap. El servidor pagina de 10 en 10: este componente solo pinta los botones y avisa
 * qué página se pidió; no corta listas ni calcula nada. Se oculta si todo cabe en una página.
 */
export function Paginacion({ paginaActual, totalPaginas, alCambiar }: PaginacionProps) {
  if (totalPaginas <= 1) {
    return null;
  }

  // Ventana de hasta 5 números alrededor de la página actual
  const inicio = Math.max(0, Math.min(paginaActual - 2, totalPaginas - MAXIMO_VISIBLES));
  const fin = Math.min(totalPaginas, inicio + MAXIMO_VISIBLES);
  const paginas = Array.from({ length: fin - inicio }, (_, i) => inicio + i);

  return (
    <nav aria-label="Paginación">
      <ul className="pagination justify-content-center">
        <li className={`page-item ${paginaActual === 0 ? 'disabled' : ''}`}>
          <button type="button" className="page-link" onClick={() => alCambiar(paginaActual - 1)}>
            Anterior
          </button>
        </li>
        {paginas.map((pagina) => (
          <li key={pagina} className={`page-item ${pagina === paginaActual ? 'active' : ''}`}>
            <button
              type="button"
              className="page-link"
              aria-current={pagina === paginaActual ? 'page' : undefined}
              onClick={() => alCambiar(pagina)}
            >
              {pagina + 1}
            </button>
          </li>
        ))}
        <li className={`page-item ${paginaActual >= totalPaginas - 1 ? 'disabled' : ''}`}>
          <button type="button" className="page-link" onClick={() => alCambiar(paginaActual + 1)}>
            Siguiente
          </button>
        </li>
      </ul>
    </nav>
  );
}
