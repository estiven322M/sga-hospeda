import type { ApartamentoResponse } from '../modelos/apartamento';
import { listarTodosLosApartamentos } from '../servicios/apartamentoServicio';
import { useConsulta } from './useConsulta';

/** Los apartamentos para llenar un <select>: `apartamentos` es null mientras carga o si falló (`error`). */
export function useApartamentos(): { apartamentos: ApartamentoResponse[] | null; error: string | null } {
  const { resultado } = useConsulta('apartamentos', (signal) => listarTodosLosApartamentos(signal));
  return {
    apartamentos: resultado?.tipo === 'datos' ? resultado.datos : null,
    error: resultado?.tipo === 'error' ? resultado.mensaje : null,
  };
}
