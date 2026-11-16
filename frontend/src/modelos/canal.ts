import type { FechaHoraISO } from './comun';

/** Espejo de TipoCanalVenta del openapi.yaml. */
export const TIPOS_CANAL = ['PLATAFORMA_EN_LINEA', 'AGENCIA_DE_VIAJES', 'OTRO'] as const;
export type TipoCanalVenta = (typeof TIPOS_CANAL)[number];

/** GET /canales: un canal externo. Nunca trae la credencial ni su hash. */
export interface CanalVentaResponse {
  codigo: string;
  nombre: string;
  tipo: TipoCanalVenta;
  activo: boolean;
}

/** POST /canales: el canal creado con su credencial en claro. Es la ÚNICA vez que el sistema la muestra. */
export interface CanalVentaCreadoResponse extends CanalVentaResponse {
  credencial: string;
}

export interface CrearCanalVentaRequest {
  codigo: string;
  nombre: string;
  tipo: TipoCanalVenta;
}

export type OperacionCanal = 'CONSULTAR_DISPONIBILIDAD' | 'CREAR_RESERVA' | 'CANCELAR_RESERVA' | 'CONSULTAR_RESERVA';
export type ResultadoCanal = 'ACEPTADA' | 'RECHAZADA' | 'REPETIDA' | 'INVALIDA' | 'NO_ENCONTRADA' | 'NO_AUTORIZADA' | 'ERROR';

/** GET /canales/bitacora: un mensaje intercambiado con un canal. */
export interface EventoCanalResponse {
  registradoEn: FechaHoraISO;
  canal: string;
  operacion: OperacionCanal;
  /** El identificador del canal (sin su código por delante); puede faltar. */
  identificadorExterno: string | null;
  resultado: ResultadoCanal;
  codigoHttp: number;
  detalle: string | null;
}
