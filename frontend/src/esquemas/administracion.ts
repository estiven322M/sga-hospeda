import { z } from 'zod';
import { CARACTERISTICAS } from '../modelos/apartamento';
import type { CrearApartamentoRequest } from '../modelos/apartamento';
import type { RegistrarPoliticaCancelacionRequest, RegistrarTemporadaRequest } from '../modelos/tarifario';
import { fechaObligatoria } from './estancia';

/**
 * Esquemas de la administración (7.3 y 7.4). Solo validan la FORMA, con los mismos límites que Bean Validation en el
 * backend. Las reglas del negocio (tarifas completas para activar, temporadas sin solape, bloqueos sobre noches
 * reservadas, política con tramo de antelación 0...) las decide el backend y responde 409 con su mensaje.
 */

const enteroPositivo = (mensaje: string) => z.string().trim().regex(/^[1-9]\d*$/, mensaje);
const enteroNoNegativo = (mensaje: string) => z.string().trim().regex(/^\d+$/, mensaje);

// ---------- Apartamento ----------
export const esquemaApartamento = z.object({
  identificacion: z.string().trim().min(1, 'Ingrese la identificación').max(30, 'La identificación no puede superar 30 caracteres'),
  nombre: z.string().trim().min(1, 'Ingrese el nombre'),
  descripcion: z.string().trim().min(1, 'Ingrese la descripción').max(500, 'La descripción no puede superar los 500 caracteres'),
  dormitorios: enteroPositivo('El apartamento debe tener al menos un dormitorio'),
  capacidad: enteroPositivo('La capacidad debe ser al menos de 1 persona'),
  // useFieldArray necesita objetos: cada elemento de la dotación es { elemento }
  dotacion: z
    .array(z.object({ elemento: z.string().trim().max(60, 'Un elemento de la dotación no puede superar los 60 caracteres') }))
    .max(30, 'La dotación admite hasta 30 elementos'),
  caracteristicas: z.array(z.enum(CARACTERISTICAS)),
});
export type FormularioApartamento = z.infer<typeof esquemaApartamento>;

/** Formulario → cuerpo de POST/PUT /apartamentos: la dotación sin renglones vacíos. */
export function solicitudDeApartamento(datos: FormularioApartamento): CrearApartamentoRequest {
  return {
    identificacion: datos.identificacion,
    nombre: datos.nombre,
    descripcion: datos.descripcion,
    dormitorios: Number(datos.dormitorios),
    capacidad: Number(datos.capacidad),
    dotacion: datos.dotacion.map(({ elemento }) => elemento).filter((elemento) => elemento !== ''),
    caracteristicas: datos.caracteristicas,
  };
}

// ---------- Bloqueo ----------
export const esquemaBloqueo = z
  .object({
    fechaInicio: fechaObligatoria('Elija la fecha de inicio'),
    fechaFin: fechaObligatoria('Elija la fecha de fin'),
    motivo: z.string().trim().min(1, 'Todo bloqueo debe justificar un motivo'),
  })
  .superRefine((datos, contexto) => {
    if (datos.fechaInicio && datos.fechaFin && datos.fechaFin <= datos.fechaInicio) {
      contexto.addIssue({ code: 'custom', path: ['fechaFin'], message: 'La fecha de fin debe ser posterior a la de inicio' });
    }
  });
export type FormularioBloqueo = z.infer<typeof esquemaBloqueo>;

// ---------- Temporada ----------
export const esquemaTemporada = z
  .object({
    nombre: z.string().trim().min(1, 'Ingrese el nombre de la temporada'),
    base: z.boolean(),
    periodos: z.array(z.object({ desde: z.string(), hasta: z.string() })),
  })
  .superRefine((datos, contexto) => {
    if (datos.base) return; // la temporada base no tiene periodos: cubre el resto del año
    if (datos.periodos.length === 0) {
      contexto.addIssue({ code: 'custom', path: ['periodos'], message: 'Una temporada que no es la base necesita al menos un periodo' });
    }
    datos.periodos.forEach((periodo, indice) => {
      if (!/^\d{4}-\d{2}-\d{2}$/.test(periodo.desde)) {
        contexto.addIssue({ code: 'custom', path: ['periodos', indice, 'desde'], message: 'Elija la fecha de inicio del periodo' });
      }
      if (!/^\d{4}-\d{2}-\d{2}$/.test(periodo.hasta)) {
        contexto.addIssue({ code: 'custom', path: ['periodos', indice, 'hasta'], message: 'Elija la fecha de fin del periodo' });
      } else if (periodo.desde && periodo.hasta < periodo.desde) {
        contexto.addIssue({ code: 'custom', path: ['periodos', indice, 'hasta'], message: 'El fin del periodo no puede ser anterior a su inicio' });
      }
    });
  });
export type FormularioTemporada = z.infer<typeof esquemaTemporada>;

export function solicitudDeTemporada(datos: FormularioTemporada): RegistrarTemporadaRequest {
  return {
    nombre: datos.nombre,
    base: datos.base,
    ...(!datos.base && { periodos: datos.periodos }),
  };
}

// ---------- Tarifa ----------
export const esquemaTarifa = z.object({
  valor: enteroPositivo('El valor de la tarifa debe ser un número entero de pesos mayor que cero'),
});

// ---------- Política de cancelación ----------
export const esquemaPolitica = z.object({
  vigenteDesde: fechaObligatoria('Elija desde cuándo rige la política'),
  tramos: z
    .array(
      z.object({
        horasAntelacionMinima: enteroNoNegativo('La antelación debe ser un número entero de horas (0 o más)'),
        porcentajeRetencion: enteroNoNegativo('El porcentaje debe ser un número entero').refine(
          (valor) => valor === '' || Number(valor) <= 100,
          'El porcentaje no puede superar 100',
        ),
      }),
    )
    .min(2, 'La política necesita al menos dos tramos'),
});
export type FormularioPolitica = z.infer<typeof esquemaPolitica>;

export function solicitudDePolitica(datos: FormularioPolitica): RegistrarPoliticaCancelacionRequest {
  return {
    vigenteDesde: datos.vigenteDesde,
    tramos: datos.tramos.map((tramo) => ({
      horasAntelacionMinima: Number(tramo.horasAntelacionMinima),
      porcentajeRetencion: Number(tramo.porcentajeRetencion),
    })),
  };
}
