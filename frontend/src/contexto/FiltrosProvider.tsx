import { useMemo, useReducer, type ReactNode } from 'react';
import type { FormularioOcupante } from '../esquemas/ocupante';
import { useAuth } from '../hooks/useAuth';
import { ContextoFiltros, type ValorFiltros } from './contextoFiltros';
import { estadoInicial, reductorFiltros } from './filtrosReductor';

/**
 * Guarda los filtros de la búsqueda FUERA de la página: al ir al detalle de un apartamento y volver, la página
 * Buscar se vuelve a crear, pero los filtros siguen aquí. Se monta con `key` distinta por usuario (App.tsx), para
 * que otra persona que ingrese en el mismo navegador no herede los filtros ni los ocupantes de la anterior.
 */
export function FiltrosProvider({ children }: { children: ReactNode }) {
  const { sesion, tieneRol } = useAuth();

  // Un huésped es, casi siempre, uno de los ocupantes: se le ofrece ya escrito con los datos de su sesión.
  const titular: FormularioOcupante | undefined =
    tieneRol('HUESPED') && sesion?.documento
      ? { documento: sesion.documento, nombre: sesion.nombre ?? '', fechaNacimiento: '' }
      : undefined;

  const [estado, dispatch] = useReducer(reductorFiltros, titular, estadoInicial);
  const valor = useMemo<ValorFiltros>(() => ({ estado, dispatch }), [estado]);

  return <ContextoFiltros value={valor}>{children}</ContextoFiltros>;
}
