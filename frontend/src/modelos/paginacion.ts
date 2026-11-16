/**
 * Espejo de PaginaBase + <Elemento>Response del openapi.yaml: la forma de TODOS los listados del backend.
 * Las páginas se numeran desde 0. NO es el PaginaResponse de la guía de referencia (paginaActual, tamanoPagina):
 * el contrato real de este backend usa `pagina` y `tamanio`.
 */
export interface PaginaResponse<T> {
  contenido: T[];
  /** Número de la página devuelta, desde 0. */
  pagina: number;
  /** Siempre 10: el tamaño lo fija el servidor, el cliente no lo elige (12.4). */
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

/** Tamaño de página de todos los listados. Solo para mostrar; el que manda es `tamanio` de la respuesta. */
export const TAMANIO_PAGINA = 10;
