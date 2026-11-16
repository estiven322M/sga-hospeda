import type {
  LoginRequest,
  RegistroUsuarioRequest,
  TokenResponse,
  UsuarioRegistradoResponse,
} from '../modelos/auth';
import { api } from './api';

/** POST /auth/login (público). Credenciales erradas: 401 con un mensaje genérico, que no revela qué correos existen. */
export async function login(credenciales: LoginRequest): Promise<TokenResponse> {
  const respuesta = await api.post<TokenResponse>('/auth/login', credenciales);
  return respuesta.data;
}

/** POST /auth/registro (público). Siempre crea un HUESPED. Correo o documento repetidos: 409 con el mensaje de la regla. */
export async function registrar(datos: RegistroUsuarioRequest): Promise<UsuarioRegistradoResponse> {
  const respuesta = await api.post<UsuarioRegistradoResponse>('/auth/registro', datos);
  return respuesta.data;
}
