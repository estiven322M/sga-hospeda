import { createContext } from 'react';
import type { LoginRequest, RolUsuario } from '../modelos/auth';

/** La sesión que ve la aplicación: lo mínimo que se puede leer del token. */
export interface Sesion {
  token: string;
  roles: RolUsuario[];
  /** El `sub` del token. El backend no entrega el nombre de la persona: se muestra el correo. */
  correo: string | null;
  /** Documento del titular: es el puente con sus reservas (el backend filtra "mis reservas" por él). */
  documento: string | null;
  /** Expiración del token en milisegundos desde 1970. */
  expiraEnMs: number | null;
}

/** Lo que el contexto pone a disposición de cualquier componente. */
export interface ValorAuth {
  sesion: Sesion | null;
  autenticado: boolean;
  iniciarSesion: (credenciales: LoginRequest) => Promise<Sesion>;
  cerrarSesion: () => void;
  /** ¿Tiene la sesión alguno de estos roles? Sin sesión, siempre false. */
  tieneRol: (...roles: RolUsuario[]) => boolean;
}

// null como valor inicial: así useAuth puede detectar que se usa fuera del proveedor
export const ContextoAuth = createContext<ValorAuth | null>(null);
