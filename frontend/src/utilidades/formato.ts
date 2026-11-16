import type { FechaHoraISO, FechaISO, HoraISO, Pesos } from '../modelos/comun';

// Un formateador se crea una vez y se reutiliza: construirlo es lo costoso.
const pesos = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0, // el peso colombiano se maneja sin decimales (3.4)
});

const fechaLarga = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
const fechaYHora = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** 480000 → "$ 480.000" (COP, sin decimales). Un saldo a favor negativo conserva el signo. */
export function formatearPesos(valor: Pesos): string {
  return pesos.format(valor);
}

/**
 * "2026-11-20" → "20 nov 2026".
 * new Date("2026-11-20") se interpreta en UTC y en Colombia (UTC-5) mostraría el 19 de noviembre. Se agrega la
 * hora SIN zona para que se interprete en la hora local del navegador y el día no se corra.
 */
export function formatearFecha(fecha: FechaISO): string {
  return fechaLarga.format(new Date(`${fecha}T00:00:00`));
}

/** "2026-11-20T10:00:00" → "20 nov 2026, 10:00 a. m.". El backend manda la hora local del alojamiento, sin zona. */
export function formatearFechaHora(momento: FechaHoraISO): string {
  return fechaYHora.format(new Date(momento));
}

/** "16:00:00" → "16:00". */
export function formatearHora(hora: HoraISO): string {
  return hora.slice(0, 5);
}

/** "2026-12-10" a "2026-12-13" → "10 dic 2026 → 13 dic 2026". */
export function formatearRango(entrada: FechaISO, salida: FechaISO): string {
  return `${formatearFecha(entrada)} → ${formatearFecha(salida)}`;
}

/** 3 → "3 noches", 1 → "1 noche". */
export function formatearNoches(noches: number): string {
  return `${noches} ${noches === 1 ? 'noche' : 'noches'}`;
}
