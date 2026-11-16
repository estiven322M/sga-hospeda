import type { Caracteristica } from '../modelos/apartamento';
import { listarCaracteristicas } from '../servicios/apartamentoServicio';
import { useConsulta } from './useConsulta';

/** El catálogo de características (GET /caracteristicas): `catalogo` es null mientras carga o si falló (`error`). */
export function useCaracteristicas(): { catalogo: Caracteristica[] | null; error: string | null } {
  const { resultado } = useConsulta('caracteristicas', (signal) => listarCaracteristicas(signal));
  return {
    catalogo: resultado?.tipo === 'datos' ? resultado.datos : null,
    error: resultado?.tipo === 'error' ? resultado.mensaje : null,
  };
}
