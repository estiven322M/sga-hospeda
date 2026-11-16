import type { AlojamientoResponse } from '../modelos/alojamiento';
import { obtenerAlojamiento } from '../servicios/alojamientoServicio';
import { useConsulta } from './useConsulta';

/**
 * Los datos públicos del alojamiento (GET /alojamiento). `alojamiento` es null mientras carga o si falló (`error`);
 * quien lo use debe seguir funcionando sin él: la pantalla no depende de que esta consulta responda.
 */
export function useAlojamiento(): {
  alojamiento: AlojamientoResponse | null;
  error: string | null;
  recargar: () => void;
} {
  const { resultado, recargar } = useConsulta('alojamiento', (signal) => obtenerAlojamiento(signal));
  return {
    alojamiento: resultado?.tipo === 'datos' ? resultado.datos : null,
    error: resultado?.tipo === 'error' ? resultado.mensaje : null,
    recargar,
  };
}
