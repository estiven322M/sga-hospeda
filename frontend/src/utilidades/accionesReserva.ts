import type { EstadoReserva } from '../modelos/reserva';

/**
 * Qué acciones se OFRECEN según el estado de la reserva (sección 8). Es una ayuda para no mostrar botones que de
 * todas formas responderían 409: quien decide si la acción es válida (anticipo, fechas, hora límite de no-show,
 * apartamento preparado...) es el backend.
 */
export const puedeCancelar = (estado: EstadoReserva) => estado === 'PENDIENTE' || estado === 'CONFIRMADA';
export const puedeModificar = (estado: EstadoReserva) => estado === 'PENDIENTE' || estado === 'CONFIRMADA';
export const puedeConfirmar = (estado: EstadoReserva) => estado === 'PENDIENTE';
export const puedeRegistrarLlegada = (estado: EstadoReserva) => estado === 'CONFIRMADA';
export const puedeDeclararNoShow = (estado: EstadoReserva) => estado === 'CONFIRMADA';
export const puedeRegistrarSalida = (estado: EstadoReserva) => estado === 'EN_CURSO';
