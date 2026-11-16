import type {
  LoginRequest,
  RegistroUsuarioRequest,
  TokenResponse,
  UsuarioRegistradoResponse,
} from '../modelos/auth';
import type {
  ConfirmarRecuperacionRequest,
  MensajeResponse,
  SolicitarRecuperacionRequest,
} from '../modelos/usuario';
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

/**
 * POST /auth/recuperacion/solicitar (público). Responde SIEMPRE lo mismo, exista o no el correo: nunca revela qué cuentas hay.
 * Si el correo es de un usuario activo, recibe un código de 6 dígitos que vale 15 minutos y sirve una vez.
 */
export async function solicitarRecuperacion(solicitud: SolicitarRecuperacionRequest): Promise<MensajeResponse> {
  const respuesta = await api.post<MensajeResponse>('/auth/recuperacion/solicitar', solicitud);
  return respuesta.data;
}

/** POST /auth/recuperacion/confirmar (público). Cualquier problema con el código o el correo es el mismo 409. */
export async function confirmarRecuperacion(solicitud: ConfirmarRecuperacionRequest): Promise<MensajeResponse> {
  const respuesta = await api.post<MensajeResponse>('/auth/recuperacion/confirmar', solicitud);
  return respuesta.data;
}
