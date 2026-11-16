import { useSyncExternalStore } from 'react';

/**
 * La foto de perfil de quien tiene la sesión, compartida entre el encabezado (menú del usuario) y la página Mi perfil: al
 * cambiarla en una, la otra se entera. Es un almacén mínimo fuera de React; useSyncExternalStore hace que los componentes se
 * vuelvan a pintar cuando cambia. La URL viene del backend (GET /usuarios/yo): aquí no se calcula nada.
 */
let fotoActual: string | null = null;
const oyentes = new Set<() => void>();

export function establecerFotoDePerfil(url: string | null): void {
  if (url === fotoActual) return;
  fotoActual = url;
  oyentes.forEach((avisar) => avisar());
}

function suscribirse(avisar: () => void): () => void {
  oyentes.add(avisar);
  return () => {
    oyentes.delete(avisar);
  };
}

/** La URL de la foto de perfil, o null si no tiene (se muestran sus iniciales). */
export function useFotoPerfil(): string | null {
  return useSyncExternalStore(suscribirse, () => fotoActual);
}
