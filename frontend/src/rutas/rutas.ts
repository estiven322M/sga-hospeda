/**
 * Todas las rutas de la aplicación en UN solo lugar. Los enlaces, el menú y App.tsx las leen de aquí:
 * si una ruta cambia, cambia en un solo sitio. Las que llevan parámetro son funciones.
 */
export const RUTAS = {
  inicio: '/',
  login: '/login',
  registro: '/registro',
  noAutorizado: '/no-autorizado',

  // Huésped
  buscar: '/buscar',
  detalleApartamento: (identificacion: string) => `/apartamentos/${identificacion}`,
  nuevaReservaHuesped: (identificacion: string) => `/reservar/${identificacion}`,
  misReservas: '/mis-reservas',
  detalleReserva: (codigo: string) => `/mis-reservas/${codigo}`,

  // Recepción
  reservas: '/recepcion/reservas',
  nuevaReservaRecepcion: '/recepcion/reservas/nueva',
  gestionReserva: (codigo: string) => `/recepcion/reservas/${codigo}`,

  // Personal de servicio
  preparacion: '/servicio/preparacion',

  // Administración
  apartamentos: '/administracion/apartamentos',
  apartamentoNuevo: '/administracion/apartamentos/nuevo',
  apartamentoEditar: (identificacion: string) => `/administracion/apartamentos/${identificacion}/editar`,
  bloqueos: '/administracion/bloqueos',
  temporadasTarifas: '/administracion/temporadas-tarifas',
  politicaCancelacion: '/administracion/politica-cancelacion',
  rechazosExternos: '/administracion/rechazos-externos',
} as const;

/** Patrones de ruta para <Route path=...>, relativos a la raíz (sin "/" inicial). Los parámetros llevan ":". */
export const PATRONES = {
  buscar: 'buscar',
  detalleApartamento: 'apartamentos/:identificacion',
  nuevaReservaHuesped: 'reservar/:identificacion',
  misReservas: 'mis-reservas',
  detalleReserva: 'mis-reservas/:codigo',

  reservas: 'recepcion/reservas',
  nuevaReservaRecepcion: 'recepcion/reservas/nueva',
  gestionReserva: 'recepcion/reservas/:codigo',

  preparacion: 'servicio/preparacion',

  apartamentos: 'administracion/apartamentos',
  apartamentoNuevo: 'administracion/apartamentos/nuevo',
  apartamentoEditar: 'administracion/apartamentos/:identificacion/editar',
  bloqueos: 'administracion/bloqueos',
  temporadasTarifas: 'administracion/temporadas-tarifas',
  politicaCancelacion: 'administracion/politica-cancelacion',
  rechazosExternos: 'administracion/rechazos-externos',
} as const;
