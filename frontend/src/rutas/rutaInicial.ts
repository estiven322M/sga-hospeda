import type { RolUsuario } from '../modelos/auth';
import { RUTAS } from './rutas';

/**
 * Página a la que se envía a cada persona al ingresar, según su rol. Si hubiera más de uno, gana el de más alcance:
 * ADMINISTRADOR, RECEPCIONISTA, PERSONAL_SERVICIO y, por último, HUESPED.
 */
export function rutaInicial(roles: readonly RolUsuario[]): string {
  if (roles.includes('ADMINISTRADOR')) return RUTAS.apartamentos;
  if (roles.includes('RECEPCIONISTA')) return RUTAS.reservas;
  if (roles.includes('PERSONAL_SERVICIO')) return RUTAS.preparacion;
  if (roles.includes('HUESPED')) return RUTAS.buscar;
  return RUTAS.inicio;
}
