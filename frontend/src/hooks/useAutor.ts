import { useAuth } from './useAuth';

/** Quién firma las acciones sobre una reserva (el `autor` de los eventos del historial): el nombre de la sesión. */
export function useAutor(): string {
  const { sesion } = useAuth();
  return sesion?.nombre ?? sesion?.correo ?? 'Desconocido';
}
