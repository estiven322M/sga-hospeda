import { useMemo, useReducer, type ReactNode } from 'react';
import { ContextoFiltrosReservas, type ValorFiltrosReservas } from './contextoFiltrosReservas';
import { estadoInicialReservas, reductorFiltrosReservas } from './filtrosReservasReductor';

/**
 * Guarda los filtros del listado de reservas de recepción fuera de la página: al abrir la gestión de una reserva y
 * volver, el listado sigue con los mismos filtros y en la misma página. Se monta con `key` por usuario (App.tsx).
 */
export function FiltrosReservasProvider({ children }: { children: ReactNode }) {
  const [estado, dispatch] = useReducer(reductorFiltrosReservas, undefined, estadoInicialReservas);
  const valor = useMemo<ValorFiltrosReservas>(() => ({ estado, dispatch }), [estado]);
  return <ContextoFiltrosReservas value={valor}>{children}</ContextoFiltrosReservas>;
}
