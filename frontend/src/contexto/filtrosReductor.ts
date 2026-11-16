import type { FormularioBusqueda } from '../esquemas/busqueda';
import { OCUPANTE_VACIO, type FormularioOcupante } from '../esquemas/ocupante';

/** Filtros de la búsqueda más la página del listado que se está viendo (desde 0). */
export interface EstadoFiltros {
  filtros: FormularioBusqueda;
  pagina: number;
}

export type AccionFiltros =
  | { tipo: 'establecer'; filtros: FormularioBusqueda }
  | { tipo: 'irAPagina'; pagina: number }
  | { tipo: 'reiniciar'; titular?: FormularioOcupante };

export function estadoInicial(titular?: FormularioOcupante): EstadoFiltros {
  return {
    filtros: {
      fechaEntrada: '',
      fechaSalida: '',
      ocupantes: [titular ?? { ...OCUPANTE_VACIO }],
      capacidadMinima: '',
      precioMinimo: '',
      precioMaximo: '',
      caracteristicas: [],
    },
    pagina: 0,
  };
}

/**
 * Reductor PURO: dado el mismo estado y la misma acción devuelve siempre lo mismo, sin leer relojes, sin llamar al
 * servidor y sin modificar el estado recibido. Por eso se puede probar sin React.
 */
export function reductorFiltros(estado: EstadoFiltros, accion: AccionFiltros): EstadoFiltros {
  switch (accion.tipo) {
    case 'establecer':
      // Si nada cambió se devuelve el MISMO estado: React no vuelve a pintar ni se repite la consulta.
      if (JSON.stringify(accion.filtros) === JSON.stringify(estado.filtros)) {
        return estado;
      }
      // Otros filtros son otra búsqueda: se vuelve a la primera página.
      return { filtros: accion.filtros, pagina: 0 };
    case 'irAPagina':
      return accion.pagina === estado.pagina ? estado : { ...estado, pagina: accion.pagina };
    case 'reiniciar':
      return estadoInicial(accion.titular);
  }
}
