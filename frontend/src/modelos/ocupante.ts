import type { FechaISO } from './comun';

/** Una persona del grupo, al pedir una cotización, una búsqueda o una reserva (OcupanteRequest). */
export interface OcupanteRequest {
  documento: string;
  nombre: string;
  /** La edad se calcula a la fecha de entrada: define quién paga (L-09) y quién es adulto responsable (RP-03). */
  fechaNacimiento: FechaISO;
}

/** Una persona del grupo de una reserva (OcupanteResponse). */
export interface OcupanteResponse {
  documento: string;
  nombre: string;
  fechaNacimiento: FechaISO;
}
