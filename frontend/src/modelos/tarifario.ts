import type { FechaISO, Pesos } from './comun';

// ---------- Temporadas (7.4) ----------

export interface PeriodoResponse {
  desde: FechaISO;
  hasta: FechaISO;
}

/** Una temporada con nombre y rangos de fechas. La temporada base no tiene periodos: cubre el resto del año. */
export interface TemporadaResponse {
  nombre: string;
  base: boolean;
  periodos: PeriodoResponse[];
}

export interface PeriodoRequest {
  desde: FechaISO;
  hasta: FechaISO;
}

/** POST /temporadas. Las temporadas no se solapan y existe una sola base. */
export interface RegistrarTemporadaRequest {
  nombre: string;
  base: boolean;
  periodos?: PeriodoRequest[];
}

// ---------- Tarifas ----------

/** Tarifa por ocupante facturable y por noche de un apartamento en una temporada. */
export interface TarifaResponse {
  temporada: string;
  valorPorOcupanteNoche: Pesos;
  /** "COP". */
  moneda: string;
}

/** PUT /apartamentos/{identificacion}/tarifas/{temporada}. */
export interface RegistrarTarifaRequest {
  /** Al menos 1 peso. */
  valorPorOcupanteNoche: Pesos;
}

// ---------- Política de cancelación (L-10) ----------

export interface TramoResponse {
  horasAntelacionMinima: number;
  porcentajeRetencion: number;
}

/** Una versión de la política. Al publicar una nueva se conserva la anterior; cada reserva queda ligada a la suya (RN-13). */
export interface PoliticaCancelacionResponse {
  version: number;
  vigenteDesde: FechaISO;
  tramos: TramoResponse[];
}

export interface TramoRequest {
  /** Mínimo 0. */
  horasAntelacionMinima: number;
  /** De 0 a 100. */
  porcentajeRetencion: number;
}

/** POST /politicas-cancelacion. Al menos 2 tramos, uno de antelación 0 (L-10). */
export interface RegistrarPoliticaCancelacionRequest {
  vigenteDesde: FechaISO;
  tramos: TramoRequest[];
}
