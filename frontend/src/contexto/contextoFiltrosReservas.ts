import { createContext, type Dispatch } from 'react';
import type { AccionFiltrosReservas, EstadoFiltrosReservas } from './filtrosReservasReductor';

export interface ValorFiltrosReservas {
  estado: EstadoFiltrosReservas;
  dispatch: Dispatch<AccionFiltrosReservas>;
}

export const ContextoFiltrosReservas = createContext<ValorFiltrosReservas | null>(null);
