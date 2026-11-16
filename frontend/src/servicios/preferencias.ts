/**
 * Datos que el navegador recuerda mientras la pestaña siga abierta (sessionStorage), para que el huésped no tenga que
 * escribir lo mismo en cada búsqueda. Se guarda SOLO la fecha de nacimiento del propio huésped, bajo una clave que
 * incluye su documento (otra persona en el mismo navegador no la hereda) y se borra al cerrar la sesión.
 * El almacenamiento puede no estar disponible (modo privado, bloqueo): todo falla en silencio.
 */
const PREFIJO = 'sga.fechaNacimiento.';

const FECHA = /^\d{4}-\d{2}-\d{2}$/;

export function recordarFechaNacimiento(documento: string, fechaNacimiento: string): void {
  if (!documento || !FECHA.test(fechaNacimiento)) return;
  try {
    sessionStorage.setItem(PREFIJO + documento, fechaNacimiento);
  } catch {
    // sin almacenamiento: simplemente no se recuerda
  }
}

export function fechaNacimientoRecordada(documento: string | null | undefined): string {
  if (!documento) return '';
  try {
    const valor = sessionStorage.getItem(PREFIJO + documento);
    return valor && FECHA.test(valor) ? valor : '';
  } catch {
    return '';
  }
}

/** Borra todo lo recordado (al cerrar la sesión). */
export function olvidarPreferencias(): void {
  try {
    for (const clave of Object.keys(sessionStorage)) {
      if (clave.startsWith(PREFIJO)) sessionStorage.removeItem(clave);
    }
  } catch {
    // sin almacenamiento: nada que borrar
  }
}
