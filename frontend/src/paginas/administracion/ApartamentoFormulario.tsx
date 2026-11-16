import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router';
import { Campo } from '../../componentes/Campo';
import { CampoAreaTexto } from '../../componentes/CampoAreaTexto';
import { Cargando } from '../../componentes/Cargando';
import { SelectorCaracteristicas } from '../../componentes/SelectorCaracteristicas';
import { esquemaApartamento, solicitudDeApartamento, type FormularioApartamento } from '../../esquemas/administracion';
import { useConsulta } from '../../hooks/useConsulta';
import type { ApartamentoResponse } from '../../modelos/apartamento';
import type { ReservaResumenResponse } from '../../modelos/reserva';
import { RUTAS } from '../../rutas/rutas';
import { formatearRango } from '../../utilidades/formato';
import { actualizarApartamento, crearApartamento, obtenerApartamento } from '../../servicios/apartamentoServicio';
import { erroresDeCampo, mensajeDeError } from '../../utilidades/errores';
import { notificarExito } from '../../utilidades/notificaciones';

const CAMPOS = ['identificacion', 'nombre', 'descripcion', 'dormitorios', 'capacidad'] as const;

function valoresIniciales(inicial: ApartamentoResponse | null): FormularioApartamento {
  return {
    identificacion: inicial?.identificacion ?? '',
    nombre: inicial?.nombre ?? '',
    descripcion: inicial?.descripcion ?? '',
    dormitorios: inicial ? String(inicial.dormitorios) : '',
    capacidad: inicial ? String(inicial.capacidad) : '',
    dotacion: (inicial?.dotacion ?? []).map((elemento) => ({ elemento })),
    caracteristicas: inicial?.caracteristicas ?? [],
  };
}

/** Crear (sin identificación en la ruta) o editar (con ella) un apartamento: es el mismo formulario. */
export default function ApartamentoFormulario() {
  const { identificacion } = useParams();
  return identificacion ? <Edicion identificacion={identificacion} /> : <Formulario inicial={null} />;
}

function Edicion({ identificacion }: { identificacion: string }) {
  const { resultado } = useConsulta(`apartamento|${identificacion}`, (signal) => obtenerApartamento(identificacion, signal));
  if (resultado === null) return <Cargando />;
  if (resultado.tipo === 'error') {
    return (
      <section>
        <h1 className="h3">Apartamento</h1>
        <div className="alert alert-danger" role="alert">
          {resultado.estadoHttp === 404 ? `No existe el apartamento ${identificacion}.` : resultado.mensaje}
        </div>
        <Link to={RUTAS.apartamentos} className="btn btn-outline-primary">Volver a los apartamentos</Link>
      </section>
    );
  }
  return <Formulario inicial={resultado.datos} />;
}

/**
 * Datos de un apartamento: identificación (solo al crear: no cambia, L-04), nombre, descripción, dormitorios,
 * capacidad, dotación (lista dinámica) y características (catálogo de GET /caracteristicas). Un apartamento nuevo
 * nace inactivo: hasta tener tarifa en todas las temporadas no se puede activar (7.4).
 */
function Formulario({ inicial }: { inicial: ApartamentoResponse | null }) {
  const navigate = useNavigate();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  // 7.3: reservas que quedaron por encima de la nueva capacidad (la actualización no se bloquea, solo se avisa)
  const [reservasPorEncima, setReservasPorEncima] = useState<ReservaResumenResponse[]>([]);
  const editando = inicial !== null;

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioApartamento>({
    resolver: zodResolver(esquemaApartamento),
    mode: 'onChange',
    defaultValues: valoresIniciales(inicial),
  });
  const dotacion = useFieldArray({ control, name: 'dotacion' });

  async function enviar(datos: FormularioApartamento) {
    setErrorServidor(null);
    setReservasPorEncima([]);
    const solicitud = solicitudDeApartamento(datos);
    let excedidas: ReservaResumenResponse[] = [];
    try {
      if (inicial) {
        const { identificacion: _inmutable, ...editables } = solicitud;
        excedidas = (await actualizarApartamento(inicial.identificacion, editables)).reservasPorEncimaDeLaCapacidad;
      } else {
        await crearApartamento(solicitud);
      }
    } catch (error) {
      // 409: el mensaje del dominio (identificación repetida, dotación con elementos repetidos...). 400: por campo.
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito(editando ? 'Apartamento actualizado' : 'Apartamento creado: queda inactivo hasta tener tarifa en todas las temporadas');
    if (excedidas.length > 0) {
      // Se quedó en la página para que el administrador vea la lista: no se navega
      setReservasPorEncima(excedidas);
      return;
    }
    void navigate(RUTAS.apartamentos);
  }

  return (
    <section className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h3 mb-3">{editando ? `Editar ${inicial.identificacion}` : 'Nuevo apartamento'}</h1>
        {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
        {reservasPorEncima.length > 0 && (
          <div className="alert alert-warning" role="alert">
            <p className="fw-semibold mb-1">
              Se actualizó el apartamento, pero {reservasPorEncima.length === 1 ? 'una reserva activa queda' : `${reservasPorEncima.length} reservas activas quedan`} por encima de la nueva capacidad.
            </p>
            <p className="small mb-2">
              Cambiar la capacidad no afecta las reservas ya creadas: conservan su grupo. Convendría contactar a sus titulares o reasignarlas desde recepción.
            </p>
            <ul className="small mb-2">
              {reservasPorEncima.map((reserva) => (
                <li key={reserva.codigo}>
                  <Link to={RUTAS.gestionReserva(reserva.codigo)}>{reserva.codigo}</Link> · {reserva.estado} · {reserva.totalOcupantes}{' '}
                  {reserva.totalOcupantes === 1 ? 'ocupante' : 'ocupantes'} · {formatearRango(reserva.fechaEntrada, reserva.fechaSalida)} · titular {reserva.nombreTitular}
                </li>
              ))}
            </ul>
            <Link to={RUTAS.apartamentos} className="btn btn-sm btn-outline-dark">Volver a los apartamentos</Link>
          </div>
        )}

        <form onSubmit={handleSubmit(enviar)} noValidate>
          <Campo
            id="identificacion"
            etiqueta="Identificación"
            readOnly={editando}
            error={errors.identificacion?.message}
            {...register('identificacion')}
          />
          <Campo id="nombre" etiqueta="Nombre" error={errors.nombre?.message} {...register('nombre')} />
          <CampoAreaTexto id="descripcion" etiqueta="Descripción" rows={3} error={errors.descripcion?.message} {...register('descripcion')} />
          <div className="row">
            <div className="col-6">
              <Campo id="dormitorios" type="number" min={1} etiqueta="Dormitorios" error={errors.dormitorios?.message} {...register('dormitorios')} />
            </div>
            <div className="col-6">
              <Campo id="capacidad" type="number" min={1} etiqueta="Capacidad (personas)" error={errors.capacidad?.message} {...register('capacidad')} />
            </div>
          </div>

          <fieldset className="mb-3">
            <legend className="form-label fs-6">Dotación</legend>
            {dotacion.fields.map((campo, indice) => (
              <div key={campo.id} className="d-flex gap-2 align-items-start">
                <div className="flex-grow-1">
                  <Campo
                    id={`dotacion-${indice}`}
                    etiqueta={`Elemento ${indice + 1}`}
                    error={errors.dotacion?.[indice]?.elemento?.message}
                    {...register(`dotacion.${indice}.elemento`)}
                  />
                </div>
                <button type="button" className="btn btn-sm btn-outline-danger mt-4" onClick={() => dotacion.remove(indice)}>
                  Quitar
                </button>
              </div>
            ))}
            {errors.dotacion?.message && <div className="text-danger small mb-2">{errors.dotacion.message}</div>}
            <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => dotacion.append({ elemento: '' })}>
              Agregar elemento
            </button>
          </fieldset>

          <Controller
            control={control}
            name="caracteristicas"
            render={({ field }) => <SelectorCaracteristicas prefijo="car" seleccionadas={field.value} alCambiar={field.onChange} />}
          />

          <div className="d-flex gap-2 mt-3">
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear apartamento'}
            </button>
            <Link to={RUTAS.apartamentos} className="btn btn-outline-secondary">Volver</Link>
          </div>
        </form>
      </div>
    </section>
  );
}
