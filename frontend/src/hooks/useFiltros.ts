import { useContext } from 'react';
import { ContextoFiltros } from '../contexto/contextoFiltros';

/** Los filtros de la búsqueda y su `dispatch`. Solo dentro de <FiltrosProvider>. */
export function useFiltros() {
  const valor = useContext(ContextoFiltros);
  if (!valor) {
    throw new Error('useFiltros debe usarse dentro de <FiltrosProvider>');
  }
  return valor;
}
