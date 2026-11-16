import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaServicioAdicional, type FormularioServicioAdicional } from '../esquemas/administracion';
import { useConsulta } from '../hooks/useConsulta';
import type { ServicioAdicionalResponse } from '../modelos/alojamiento';
import {
  activarServicioAdicional,
  actualizarServicioAdicional,
  crearServicioAdicional,
  darDeBajaServicioAdicional,
  listarServiciosAdicionales,
} from '../servicios/servicioAdicionalServicio';
import { erroresDeCampo, mensajeDeError } from '../utilidades/errores';
import { formatearPesos } from '../utilidades/formato';
import { confirmarAccion, notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { CampoCasilla } from './CampoCasilla';
import { Cargando } from './Cargando';
import { Paginacion } from './Paginacion';

const CAMPOS = ['codigo', 'nombre', 'descripcion', 'generaCargo', 'valor'] as const;

const VACIO: FormularioServicioAdicional = { codigo: '', nombre: '', descripcion: '', generaCargo: true, valor: '' };

interface FormularioServicioProps {
  /** Servicio que se edita; null para crear uno nuevo. */
  servicio: ServicioAdicionalResponse | null;
  alTerminar: () => void;
  alCancelar: () => void;
}

/** Crear o editar un servicio adicional. El código se escribe al crear y no cambia después. */
function FormularioServicio({ servicio, alTerminar, alCancelar }: FormularioServicioProps) {
  const editando = servicio !== null;
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioServicioAdicional>({
    resolver: zodResolver(esquemaServicioAdicional),
    mode: 'onChange',
    defaultValues: servicio
      ? { codigo: servicio.codigo, nombre: servicio.nombre, descripcion: servicio.descripcion, generaCargo: servicio.generaCargo, valor: String(servicio.valor) }
      : VACIO,
  });

  async function enviar(datos: FormularioServicioAdicional) {
    setErrorServidor(null);
    const cuerpo = { nombre: datos.nombre, descripcion: datos.descripcion, generaCargo: datos.generaCargo, valor: Number(datos.valor) };
    try {
      if (servicio) await actualizarServicioAdicional(servicio.codigo, cuerpo);
      else await crearServicioAdicional({ codigo: datos.codigo, ...cuerpo });
    } catch (error) {
      setErrorServidor(mensajeDeError(error)); // 409: código repetido o valor incoherente con "genera cargo"
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
      return;
    }
    notificarExito(editando ? 'Servicio adicional actualizado' : 'Servicio adicional creado');
    alTerminar();
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label={editando ? 'Editar servicio adicional' : 'Nuevo servicio adicional'} className="card card-body mb-3">
      <h3 className="h6">{editando ? `Editar ${servicio.codigo}` : 'Nuevo servicio adicional'}</h3>
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <div className="row">
        <div className="col-12 col-md-3">
          <Campo id="serv-codigo" etiqueta="Código" readOnly={editando} error={errors.codigo?.message} {...register('codigo')} />
        </div>
        <div className="col-12 col-md-5">
          <Campo id="serv-nombre" etiqueta="Nombre" error={errors.nombre?.message} {...register('nombre')} />
        </div>
        <div className="col-12 col-md-4">
          <Campo id="serv-valor" type="number" min={0} etiqueta="Valor (pesos)" error={errors.valor?.message} {...register('valor')} />
        </div>
      </div>
      <Campo id="serv-descripcion" etiqueta="Descripción" error={errors.descripcion?.message} {...register('descripcion')} />
      <div className="mb-3">
        <CampoCasilla id="serv-generaCargo" etiqueta="Genera cargo en el folio (si no, es una cortesía y su valor es 0)" {...register('generaCargo')} />
      </div>
      <div className="d-flex gap-2">
        <button type="submit" className="btn btn-primary btn-sm" disabled={isSubmitting}>
          {isSubmitting ? 'Guardando…' : 'Guardar'}
        </button>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}

/**
 * Gestión de los servicios adicionales (L-17): lista de 10 en 10, crear, editar y dar de baja o de alta (baja lógica: un
 * servicio de baja deja de poderse cargar pero no se borra). Las reglas las decide el backend y su 409 se muestra tal cual.
 */
export function GestionServiciosAdicionales() {
  const [pagina, setPagina] = useState(0);
  const [formulario, setFormulario] = useState<{ servicio: ServicioAdicionalResponse | null } | null>(null);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);
  const { resultado, cargando, recargar } = useConsulta(`servicios|${pagina}`, (signal) => listarServiciosAdicionales(false, pagina, signal));

  async function cambiarActividad(servicio: ServicioAdicionalResponse) {
    setErrorAccion(null);
    if (servicio.activo) {
      const aceptada = await confirmarAccion(
        'Dar de baja el servicio',
        `"${servicio.nombre}" dejará de poderse cargar a los folios. Es una baja lógica: el servicio y los cargos ya hechos se conservan, y se puede volver a dar de alta.`,
      );
      if (!aceptada) return;
    }
    try {
      if (servicio.activo) await darDeBajaServicioAdicional(servicio.codigo);
      else await activarServicioAdicional(servicio.codigo);
    } catch (error) {
      setErrorAccion(mensajeDeError(error));
      return;
    }
    notificarExito(servicio.activo ? 'Servicio dado de baja' : 'Servicio activado');
    recargar();
  }

  function terminarFormulario() {
    setFormulario(null);
    recargar();
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-2">
        <p className="text-body-secondary small mb-0">Los servicios activos se pueden cargar al folio de una reserva confirmada o en curso.</p>
        {!formulario && (
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setFormulario({ servicio: null })}>
            Nuevo servicio
          </button>
        )}
      </div>

      {formulario && (
        <FormularioServicio
          key={formulario.servicio?.codigo ?? 'nuevo'}
          servicio={formulario.servicio}
          alTerminar={terminarFormulario}
          alCancelar={() => setFormulario(null)}
        />
      )}
      {errorAccion && <div className="alert alert-warning py-2" role="alert">{errorAccion}</div>}

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">Aún no hay servicios adicionales.</div>
          ) : (
            <>
              {resultado.datos.contenido.map((servicio) => (
                <article key={servicio.codigo} className="card mb-2">
                  <div className="card-body py-2 d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <div>
                      <div className="fw-semibold">
                        {servicio.nombre} <span className="badge text-bg-light border">{servicio.codigo}</span>{' '}
                        <span className={`badge ${servicio.activo ? 'text-bg-success' : 'text-bg-secondary'}`}>{servicio.activo ? 'Activo' : 'De baja'}</span>
                      </div>
                      <div className="small text-body-secondary">
                        {servicio.generaCargo ? formatearPesos(servicio.valor) : 'Cortesía: no genera cargo'}
                        {servicio.descripcion && ` · ${servicio.descripcion}`}
                      </div>
                    </div>
                    <div className="d-flex gap-2">
                      <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => setFormulario({ servicio })}>Editar</button>
                      <button
                        type="button"
                        className={`btn btn-sm ${servicio.activo ? 'btn-outline-danger' : 'btn-outline-success'}`}
                        onClick={() => void cambiarActividad(servicio)}
                      >
                        {servicio.activo ? 'Dar de baja' : 'Activar'}
                      </button>
                    </div>
                  </div>
                </article>
              ))}
              <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
            </>
          ))}
      </div>
    </div>
  );
}
