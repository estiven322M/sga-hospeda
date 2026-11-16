import { matchPath } from 'react-router';
import type { RolUsuario } from '../modelos/auth';
import { PATRONES } from './rutas';

/**
 * Qué rol puede abrir cada página, según la matriz de acceso de docs/seguridad.md (3). Es una AYUDA de navegación
 * para no mostrar pantallas que de todas formas responderían 403: quien decide es el backend con @PreAuthorize.
 * App.tsx arma sus grupos con estas constantes y puedeAcceder() usa las mismas reglas: una sola fuente de verdad.
 */

/** Cotizar y buscar disponibilidad (POST /cotizaciones y /disponibilidad): todos menos el personal de servicio. */
export const ROLES_BUSQUEDA: readonly RolUsuario[] = ['HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR'];

/** Reservar por el PORTAL y ver "mis reservas": solo el huésped (el backend filtra por el documento del token). */
export const ROLES_HUESPED: readonly RolUsuario[] = ['HUESPED'];

/** Operar las reservas: listar, crear por DIRECTO, confirmar, cobrar, llegada, salida... */
export const ROLES_RECEPCION: readonly RolUsuario[] = ['RECEPCIONISTA', 'ADMINISTRADOR'];

/** Preparar apartamentos (iniciar-preparacion y marcar-preparado). */
export const ROLES_SERVICIO: readonly RolUsuario[] = ['PERSONAL_SERVICIO', 'ADMINISTRADOR'];

/** Inventario, bloqueos, temporadas, tarifas, política de cancelación y rechazos del canal externo. */
export const ROLES_ADMINISTRACION: readonly RolUsuario[] = ['ADMINISTRADOR'];

interface ReglaDeAcceso {
  patron: string;
  roles: readonly RolUsuario[];
}

const REGLAS: readonly ReglaDeAcceso[] = [
  { patron: PATRONES.buscar, roles: ROLES_BUSQUEDA },
  { patron: PATRONES.detalleApartamento, roles: ROLES_BUSQUEDA },

  { patron: PATRONES.nuevaReservaHuesped, roles: ROLES_HUESPED },
  { patron: PATRONES.misReservas, roles: ROLES_HUESPED },
  { patron: PATRONES.detalleReserva, roles: ROLES_HUESPED },

  { patron: PATRONES.reservas, roles: ROLES_RECEPCION },
  { patron: PATRONES.nuevaReservaRecepcion, roles: ROLES_RECEPCION },
  { patron: PATRONES.gestionReserva, roles: ROLES_RECEPCION },

  { patron: PATRONES.preparacion, roles: ROLES_SERVICIO },

  { patron: PATRONES.apartamentos, roles: ROLES_ADMINISTRACION },
  { patron: PATRONES.apartamentoNuevo, roles: ROLES_ADMINISTRACION },
  { patron: PATRONES.apartamentoEditar, roles: ROLES_ADMINISTRACION },
  { patron: PATRONES.bloqueos, roles: ROLES_ADMINISTRACION },
  { patron: PATRONES.temporadasTarifas, roles: ROLES_ADMINISTRACION },
  { patron: PATRONES.politicaCancelacion, roles: ROLES_ADMINISTRACION },
  { patron: PATRONES.rechazosExternos, roles: ROLES_ADMINISTRACION },
];

/** ¿Puede alguno de estos roles abrir esta ruta? Una ruta sin regla (inicio, login...) es de acceso libre. */
export function puedeAcceder(ruta: string, roles: readonly RolUsuario[]): boolean {
  const regla = REGLAS.find((candidata) => matchPath({ path: `/${candidata.patron}`, end: true }, ruta));
  return regla === undefined || regla.roles.some((rol) => roles.includes(rol));
}
