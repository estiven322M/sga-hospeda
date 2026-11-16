import type { HoraISO, Pesos } from './comun';

/** GET /alojamiento (público): los datos del alojamiento (7.3). `mediosPago` es configuración del sistema (L-18), de solo lectura. */
export interface AlojamientoResponse {
  nombre: string;
  descripcion: string;
  ciudad: string;
  direccion: string;
  latitud: number;
  longitud: number;
  horaEntrada: HoraISO;
  /** Debe ser anterior a la hora de entrada. */
  horaSalida: HoraISO;
  normasConvivencia: string;
  /** Decisión explícita sobre las mascotas (L-16). */
  politicaMascotas: string;
  /** Medios de pago aceptados (L-18), p. ej. EFECTIVO, TARJETA, TRANSFERENCIA. */
  mediosPago: string[];
}

/** PUT /alojamiento (ADMINISTRADOR). Los medios de pago no se editan aquí. */
export interface ActualizarAlojamientoRequest {
  nombre: string;
  descripcion: string;
  ciudad: string;
  direccion: string;
  latitud: number;
  longitud: number;
  horaEntrada: HoraISO;
  horaSalida: HoraISO;
  normasConvivencia: string;
  politicaMascotas: string;
}

/** GET /alojamiento/parametros (ADMINISTRADOR): los parámetros de la sección 6, en solo lectura. */
export interface ParametrosResponse {
  umbralEdadFacturableAnios: number;
  tiempoPreparacionHoras: number;
  plazoConfirmacionHoras: number;
  horaLimiteNoShow: HoraISO;
  anticipoPorcentaje: number;
  /** Mínimo de noches por nombre de temporada (RP-01). */
  estanciaMinimaPorTemporada: Record<string, number>;
  nochesDescuentoEstanciaLarga: number;
  porcentajeDescuentoEstanciaLarga: number;
  edadAdultoResponsableAnios: number;
  mediosPago: string[];
  calendarioMaximoNoches: number;
  zonaHoraria: string;
  /** Aviso de que son configuración del sistema. */
  nota: string;
}

/** Un servicio adicional (L-17). `valor` en pesos COP; `activo` indica si se puede cargar a un folio (la baja es lógica). */
export interface ServicioAdicionalResponse {
  codigo: string;
  nombre: string;
  descripcion: string;
  generaCargo: boolean;
  valor: Pesos;
  moneda: string;
  activo: boolean;
}

/** POST /servicios-adicionales. Genera cargo ⇒ valor > 0; cortesía ⇒ valor 0. */
export interface CrearServicioAdicionalRequest {
  codigo: string;
  nombre: string;
  descripcion?: string;
  generaCargo: boolean;
  valor: Pesos;
}

/** PUT /servicios-adicionales/{codigo}: el código no cambia. */
export interface ActualizarServicioAdicionalRequest {
  nombre: string;
  descripcion?: string;
  generaCargo: boolean;
  valor: Pesos;
}

/** POST /reservas/{codigo}/folio/cargos (L-17, 7.7). */
export interface RegistrarCargoServicioRequest {
  codigoServicio: string;
  /** De 1 a 100. */
  cantidad: number;
}
