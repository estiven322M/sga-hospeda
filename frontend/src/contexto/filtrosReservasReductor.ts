import type { FiltrosListadoReservas } from '../servicios/reservaServicio';

/** Filtros del listado de reservas de recepción más la página que se está viendo (desde 0). */
export interface EstadoFiltrosReservas {
  filtros: FiltrosListadoReservas;
  pagina: number;
}

export type AccionFiltrosReservas =
  | { tipo: 'establecer'; filtros: FiltrosListadoReservas }
  | { tipo: 'irAPagina'; pagina: number }
  | { tipo: 'reiniciar' };

export const FILTROS_RESERVAS_VACIOS: FiltrosListadoReservas = {
  estado: '',
  nombreTitular: '',
  apartamento: '',
  canal: '',
  entradaDesde: '',
  entradaHasta: '',
  salidaDesde: '',
  salidaHasta: '',
};

export function estadoInicialReservas(): EstadoFiltrosReservas {
  return { filtros: { ...FILTROS_RESERVAS_VACIOS }, pagina: 0 };
}

/** Reductor PURO (mismo criterio que el de la búsqueda): sin relojes, sin servidor y sin modificar el estado recibido. */
export function reductorFiltrosReservas(estado: EstadoFiltrosReservas, accion: AccionFiltrosReservas): EstadoFiltrosReservas {
  switch (accion.tipo) {
    case 'establecer':
      // Sin cambios se devuelve el MISMO estado: no se repite la consulta ni se pierde la página.
      if (JSON.stringify(accion.filtros) === JSON.stringify(estado.filtros)) {
        return estado;
      }
      return { filtros: accion.filtros, pagina: 0 }; // otros filtros son otra lista
    case 'irAPagina':
      return accion.pagina === estado.pagina ? estado : { ...estado, pagina: accion.pagina };
    case 'reiniciar':
      return estadoInicialReservas();
  }
}
