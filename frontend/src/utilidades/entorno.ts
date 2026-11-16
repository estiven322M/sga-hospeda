/**
 * Edad desde la que alguien cuenta como adulto responsable, tomada de VITE_EDAD_ADULTO_RESPONSABLE. La regla real
 * (RP-03) y su umbral viven en el backend: este valor SOLO sirve para avisar antes de enviar. Si la variable falta o
 * no es un entero positivo vale null y el aviso no se muestra (no se inventa un umbral).
 */
function leerEdadAdultoResponsable(): number | null {
  const valor = Number(import.meta.env.VITE_EDAD_ADULTO_RESPONSABLE);
  return Number.isInteger(valor) && valor > 0 ? valor : null;
}

export const EDAD_ADULTO_RESPONSABLE: number | null = leerEdadAdultoResponsable();
