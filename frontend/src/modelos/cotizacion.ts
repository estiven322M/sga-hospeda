import type { FechaISO, Pesos } from './comun';
import type { Caracteristica } from './apartamento';
import type { OcupanteRequest } from './ocupante';

/** Una noche del desglose (RN-05): la tarifa de su temporada por los ocupantes facturables. */
export interface CargoNocheResponse {
  noche: FechaISO;
  /** Nombre de la temporada (p. ej. BASE, MEDIA, ALTA): es un dato, no un enum. */
  temporada: string;
  tarifaPorOcupante: Pesos;
  ocupantesFacturables: number;
  subtotal: Pesos;
}

/** POST /cotizaciones (CU-01). */
export interface CotizarEstanciaRequest {
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  /** Al menos 1. */
  ocupantes: OcupanteRequest[];
}

/**
 * Cotización noche por noche. `total` = `subtotalBruto` − `descuento` (RP-02: desde 7 noches se descuenta un
 * porcentaje, una sola vez sobre el total). El desglose siempre muestra el valor ANTES del descuento.
 */
export interface CotizacionResponse {
  identificacionApartamento: string;
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  noches: number;
  desglose: CargoNocheResponse[];
  subtotalBruto: Pesos;
  /** De 0 a 100. */
  porcentajeDescuento: number;
  descuento: Pesos;
  total: Pesos;
  /** "COP". */
  moneda: string;
}

/** POST /disponibilidad (CU-02). Los filtros son opcionales; las características se piden TODAS a la vez. */
export interface ConsultarDisponibilidadRequest {
  fechaEntrada: FechaISO;
  fechaSalida: FechaISO;
  /** Al menos 1. */
  ocupantes: OcupanteRequest[];
  capacidadMinima?: number;
  /** Pesos COP sobre el total de la estancia. */
  precioMinimo?: Pesos;
  precioMaximo?: Pesos;
  caracteristicas?: Caracteristica[];
}

/** Una tarjeta de resultado de la búsqueda: un apartamento libre y lo que costaría. */
export interface ApartamentoDisponibleResponse {
  identificacion: string;
  nombre: string;
  descripcion: string;
  dormitorios: number;
  capacidad: number;
  caracteristicas: Caracteristica[];
  subtotalBruto: Pesos;
  porcentajeDescuento: number;
  descuento: Pesos;
  total: Pesos;
  moneda: string;
  desglose: CargoNocheResponse[];
}
