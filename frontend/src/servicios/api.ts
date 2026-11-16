import axios from 'axios';

/**
 * Cliente HTTP ÚNICO de la aplicación: todos los servicios lo usan, nadie crea otra instancia de Axios.
 * La URL no está escrita aquí: Vite la toma de .env.development o .env.production (VITE_API_URL, con /api),
 * así que los servicios escriben rutas relativas (/reservas, /auth/login...).
 *
 * Todavía sin interceptores: el token en cada petición y el manejo del 401 llegan con la sesión (paso J2).
 * Sin 'Content-Type' fijo: Axios pone application/json para objetos y deja que el navegador arme
 * multipart/form-data si algún día se sube un archivo.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10_000,
});
