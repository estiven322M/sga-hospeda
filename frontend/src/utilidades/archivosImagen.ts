/**
 * Validación de imágenes ANTES de subirlas (Guía 21): evita enviar 40 MB al backend para que responda "solo JPG, PNG o WEBP".
 * Es una AYUDA: el backend vuelve a validar (reconoce el formato por el contenido del archivo y aplica el tamaño máximo
 * configurado), y la validación del cliente se puede saltar.
 */
export const TIPOS_DE_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'] as const;

/** Lo que se pasa a <input accept=...>: filtra el selector de archivos del sistema (el usuario puede cambiarlo). */
export const ACEPTAR_IMAGENES = TIPOS_DE_IMAGEN.join(',');

/** Tamaños máximos que avisa la pantalla; los reales son parámetros del backend (5 MB y 2 MB por omisión). */
export const MAXIMO_APARTAMENTO_MB = 5;
export const MAXIMO_PERFIL_MB = 2;

const MEGABYTE = 1024 * 1024;

/**
 * Revisa los archivos elegidos. Devuelve el primer problema encontrado, o null si todo está bien.
 * @param cupo cuántas imágenes más admite el destino (por ejemplo, las que le faltan al apartamento para llegar a 10)
 */
export function validarArchivosDeImagen(archivos: readonly File[], maximoMb: number, cupo: number): string | null {
  if (archivos.length === 0) return 'Elija al menos una imagen';
  if (archivos.length > cupo) {
    return cupo <= 0
      ? 'El apartamento ya tiene el máximo de imágenes: retire alguna antes de subir otra'
      : `Solo caben ${cupo} ${cupo === 1 ? 'imagen más' : 'imágenes más'}: elija menos archivos`;
  }
  for (const archivo of archivos) {
    if (!(TIPOS_DE_IMAGEN as readonly string[]).includes(archivo.type)) {
      return `${archivo.name} no es una imagen JPG, PNG o WEBP`;
    }
    if (archivo.size > maximoMb * MEGABYTE) {
      return `${archivo.name} pesa más de ${maximoMb} MB`;
    }
  }
  return null;
}

/** Las iniciales de un nombre (hasta dos letras) para mostrar cuando no hay foto: "Laura Gómez" → "LG". */
export function iniciales(nombre: string | null | undefined): string {
  const palabras = (nombre ?? '').trim().split(/\s+/).filter((palabra) => palabra.length > 0);
  if (palabras.length === 0) return '?';
  const primera = palabras[0] ?? '';
  const ultima = palabras.length > 1 ? (palabras[palabras.length - 1] ?? '') : '';
  return `${primera.charAt(0)}${ultima.charAt(0)}`.toUpperCase();
}
