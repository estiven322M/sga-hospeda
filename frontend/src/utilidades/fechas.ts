import type { FechaISO } from '../modelos/comun';

/**
 * Fecha de hoy como "AAAA-MM-DD" en la hora LOCAL del navegador.
 * No use new Date().toISOString(): devuelve la fecha en UTC, y en Colombia (UTC-5) a partir de las 7 p. m. ya
 * es "mañana" en UTC.
 */
export function hoyISO(): FechaISO {
  return aISO(new Date());
}

/** Suma (o resta) días a una fecha "AAAA-MM-DD" sin pasar por UTC. */
export function sumarDias(fecha: FechaISO, dias: number): FechaISO {
  const resultado = new Date(`${fecha}T00:00:00`);
  resultado.setDate(resultado.getDate() + dias);
  return aISO(resultado);
}

/** Noches entre dos fechas: la estancia es [entrada, salida), la noche de la salida no se cuenta (3.1). */
export function contarNoches(entrada: FechaISO, salida: FechaISO): number {
  const milisegundosPorDia = 24 * 60 * 60 * 1000;
  const diferencia = new Date(`${salida}T00:00:00`).getTime() - new Date(`${entrada}T00:00:00`).getTime();
  return Math.round(diferencia / milisegundosPorDia);
}

function aISO(fecha: Date): FechaISO {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}
