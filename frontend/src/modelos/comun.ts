// Tipos de texto que el backend usa para fechas. Viajan como texto ISO-8601 en el JSON (no como Date):
// convertirlos es trabajo de utilidades/formato.ts, para no sufrir corrimientos de zona horaria.

/** "2026-11-20" (formato `date` del openapi.yaml). */
export type FechaISO = string;

/** "2026-11-20T10:00:00" (formato `date-time`). */
export type FechaHoraISO = string;

/** "16:00:00" (formato `time`). */
export type HoraISO = string;

/** Pesos colombianos, enteros y sin decimales (3.4). Nunca se opera con decimales. */
export type Pesos = number;
