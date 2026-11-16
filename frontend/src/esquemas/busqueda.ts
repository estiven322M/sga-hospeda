import { z, type ZodError } from 'zod';
import { CARACTERISTICAS } from '../modelos/apartamento';
import type { ConsultarDisponibilidadRequest } from '../modelos/cotizacion';
import { estanciaConGrupo, validarEstancia } from './estancia';

/** Número entero opcional escrito en un <input>: vacío o solo dígitos. Así el filtro vacío no se envía. */
const enteroOpcional = (mensaje: string) =>
  z.string().trim().refine((valor) => valor === '' || /^\d+$/.test(valor), mensaje);

export const esquemaBusqueda = estanciaConGrupo
  .extend({
    capacidadMinima: enteroOpcional('La capacidad debe ser un número entero'),
    precioMinimo: enteroOpcional('El precio debe ser un número entero de pesos'),
    precioMaximo: enteroOpcional('El precio debe ser un número entero de pesos'),
    caracteristicas: z.array(z.enum(CARACTERISTICAS)),
  })
  .superRefine((datos, contexto) => {
    validarEstancia(datos, contexto);
    if (datos.precioMinimo !== '' && datos.precioMaximo !== '' && Number(datos.precioMinimo) > Number(datos.precioMaximo)) {
      contexto.addIssue({ code: 'custom', path: ['precioMaximo'], message: 'El precio máximo no puede ser menor que el mínimo' });
    }
  });

/** Los filtros tal como los escribe la persona (los números son texto hasta que se envían). */
export type FormularioBusqueda = z.infer<typeof esquemaBusqueda>;

/** Filtros → cuerpo de POST /disponibilidad: lo vacío no se envía. */
export function solicitudDeBusqueda(filtros: FormularioBusqueda): ConsultarDisponibilidadRequest {
  return {
    fechaEntrada: filtros.fechaEntrada,
    fechaSalida: filtros.fechaSalida,
    ocupantes: filtros.ocupantes.map(({ documento, nombre, fechaNacimiento }) => ({ documento, nombre, fechaNacimiento })),
    ...(filtros.capacidadMinima !== '' && { capacidadMinima: Number(filtros.capacidadMinima) }),
    ...(filtros.precioMinimo !== '' && { precioMinimo: Number(filtros.precioMinimo) }),
    ...(filtros.precioMaximo !== '' && { precioMaximo: Number(filtros.precioMaximo) }),
    ...(filtros.caracteristicas.length > 0 && { caracteristicas: filtros.caracteristicas }),
  };
}

const CAMPO_DEL_OCUPANTE = {
  documento: 'el documento',
  nombre: 'el nombre',
  fechaNacimiento: 'la fecha de nacimiento',
} as const;

/**
 * Qué falta (o qué está mal) para poder buscar, dicho con precisión: "Falta la fecha de nacimiento del ocupante 1".
 * Un campo vacío es "Falta ..."; uno con un valor que no sirve conserva el mensaje de su regla.
 */
export function faltantesDeBusqueda(error: ZodError<FormularioBusqueda>, filtros: FormularioBusqueda): string[] {
  const mensajes = new Set<string>();
  for (const problema of error.issues) {
    const [campo, indice, subcampo] = problema.path;
    if (campo === 'fechaEntrada' || campo === 'fechaSalida') {
      const nombre = campo === 'fechaEntrada' ? 'entrada' : 'salida';
      mensajes.add(filtros[campo] === '' ? `Falta la fecha de ${nombre}` : problema.message);
    } else if (campo === 'ocupantes' && typeof indice === 'number' && typeof subcampo === 'string' && subcampo in CAMPO_DEL_OCUPANTE) {
      const clave = subcampo as keyof typeof CAMPO_DEL_OCUPANTE;
      const valor = filtros.ocupantes[indice]?.[clave] ?? '';
      mensajes.add(valor.trim() === '' ? `Falta ${CAMPO_DEL_OCUPANTE[clave]} del ocupante ${indice + 1}` : `Ocupante ${indice + 1}: ${problema.message}`);
    } else {
      mensajes.add(problema.message);
    }
  }
  return [...mensajes];
}
