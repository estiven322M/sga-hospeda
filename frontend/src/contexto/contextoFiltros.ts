import { createContext, type Dispatch } from 'react';
import type { AccionFiltros, EstadoFiltros } from './filtrosReductor';

export interface ValorFiltros {
  estado: EstadoFiltros;
  dispatch: Dispatch<AccionFiltros>;
}

export const ContextoFiltros = createContext<ValorFiltros | null>(null);
