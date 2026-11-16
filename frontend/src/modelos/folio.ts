import type { FechaISO, Pesos } from './comun';

/** Un cargo del folio (el alojamiento, una penalidad...). */
export interface CargoFolioResponse {
  concepto: string;
  valor: Pesos;
}

/** Un ajuste del folio: movimiento con signo que corrige sin borrar (RN-16). */
export interface AjusteFolioResponse {
  concepto: string;
  valor: Pesos;
}

export interface PagoFolioResponse {
  medio: string;
  fecha: FechaISO;
  valor: Pesos;
}

/**
 * GET /reservas/{codigo}/folio y respuesta de pagos y cierre.
 * `saldo` = cargos + ajustes − pagos; negativo es saldo a favor del huésped (RN-15).
 */
export interface FolioResponse {
  codigoReserva: string;
  cargos: CargoFolioResponse[];
  ajustes: AjusteFolioResponse[];
  pagos: PagoFolioResponse[];
  saldo: Pesos;
  moneda: string;
  cerrado: boolean;
  /** Quién autorizó y por qué se cerró con saldo (CU-11). null si no hubo autorización. */
  autorizacionCierre: string | null;
}

/** POST /reservas/{codigo}/folio/pagos (CU-08). */
export interface RegistrarPagoRequest {
  /** Medio de pago en texto libre (p. ej. TRANSFERENCIA, EFECTIVO). */
  medio: string;
  fecha: FechaISO;
  /** Mayor que cero. */
  valor: Pesos;
}

/** PUT /reservas/{codigo}/folio/cerrar. Con saldo pendiente exige `autorizadoPor` y `motivo`, y solo el administrador (CU-11). */
export interface CerrarFolioRequest {
  autorizadoPor?: string;
  motivo?: string;
}
