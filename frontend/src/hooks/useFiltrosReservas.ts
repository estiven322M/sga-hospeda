import { useContext } from 'react';
import { ContextoFiltrosReservas } from '../contexto/contextoFiltrosReservas';

/** Los filtros del listado de reservas de recepción y su `dispatch`. Solo dentro de <FiltrosReservasProvider>. */
export function useFiltrosReservas() {
  const valor = useContext(ContextoFiltrosReservas);
  if (!valor) {
    throw new Error('useFiltrosReservas debe usarse dentro de <FiltrosReservasProvider>');
  }
  return valor;
}
