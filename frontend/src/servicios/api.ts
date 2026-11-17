import axios from 'axios';
import { obtenerToken } from './sesion';

/**
 * Cliente HTTP ÚNICO de la aplicación: todos los servicios lo usan, nadie crea otra instancia de Axios ni escribe
 * el encabezado Authorization a mano. La URL no está escrita aquí: Vite la toma de VITE_API_URL (con /api; en Vercel se
 * define como variable del proyecto). Sin ella se usa '/api', que en desarrollo atiende el proxy de vite.config.ts. Así que los servicios escriben rutas relativas (/reservas, /auth/login...).
 * Sin 'Content-Type' fijo: Axios pone application/json para objetos y deja que el navegador arme multipart/form-data
 * si algún día se sube un archivo.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  // 70 s: el plan gratuito de Render duerme el API y su arranque en frío puede tardar más de un minuto
  timeout: 70_000,
});

/** El login y el registro son públicos: no llevan token (uno vencido haría que el backend los rechazara con 401). */
function esRutaDeAutenticacion(url: string | undefined): boolean {
  return url?.startsWith('/auth/') ?? false;
}

// ---------- Interceptor de PETICIÓN: el token viaja en TODAS las peticiones ----------
api.interceptors.request.use((config) => {
  const token = obtenerToken();
  if (token && !esRutaDeAutenticacion(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ---------- Interceptor de RESPUESTA: el 401 y el 403 se manejan en UN solo lugar ----------
// Axios no vive dentro de React: no puede usar hooks ni navegar. El AuthProvider le deja aquí las funciones que
// saben cerrar la sesión y cambiar de página.
export interface ManejadoresDeAcceso {
  /** El backend respondió 401: no hay sesión válida (token vencido o inválido). */
  alNoAutenticado: () => void;
  /** El backend respondió 403: hay sesión, pero el rol o el propietario no lo permiten. */
  alProhibido: () => void;
}

let manejadores: ManejadoresDeAcceso | null = null;

/** Registra los manejadores; devuelve la función que los quita (la usa el efecto de React al desmontar). */
export function registrarManejadores(nuevos: ManejadoresDeAcceso): () => void {
  manejadores = nuevos;
  return () => {
    if (manejadores === nuevos) manejadores = null;
  };
}

api.interceptors.response.use(
  (respuesta) => respuesta, // 2xx: pasa sin cambios
  (error: unknown) => {
    if (axios.isAxiosError(error) && !esRutaDeAutenticacion(error.config?.url)) {
      // Un 401 del login son credenciales malas, no una sesión vencida: ese lo muestra el formulario.
      if (error.response?.status === 401) manejadores?.alNoAutenticado();
      if (error.response?.status === 403) manejadores?.alProhibido();
    }
    return Promise.reject(error); // el componente que llamó sigue recibiendo el error
  },
);
