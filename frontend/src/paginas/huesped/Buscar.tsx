import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useMemo, useState } from 'react';
import { FormProvider, useForm, useWatch } from 'react-hook-form';
import { CamposOcupantes } from '../../componentes/CamposOcupantes';
import { Campo } from '../../componentes/Campo';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { TarjetaApartamento } from '../../componentes/TarjetaApartamento';
import { esquemaBusqueda, faltantesDeBusqueda, solicitudDeBusqueda, type FormularioBusqueda } from '../../esquemas/busqueda';
import { useAuth } from '../../hooks/useAuth';
import { useDebounce } from '../../hooks/useDebounce';
import { useFiltros } from '../../hooks/useFiltros';
import type { Caracteristica } from '../../modelos/apartamento';
import type { ApartamentoDisponibleResponse } from '../../modelos/cotizacion';
import type { PaginaResponse } from '../../modelos/paginacion';
import { listarCaracteristicas } from '../../servicios/apartamentoServicio';
import { recordarFechaNacimiento } from '../../servicios/preferencias';
import { consultarDisponibilidad } from '../../servicios/disponibilidadServicio';
import { fueCancelada, mensajeDeError } from '../../utilidades/errores';
import { etiquetarCaracteristica } from '../../utilidades/etiquetas';
import { hoyISO } from '../../utilidades/fechas';

type Respuesta =
  | { tipo: 'resultado'; pagina: PaginaResponse<ApartamentoDisponibleResponse> }
  | { tipo: 'error'; mensaje: string };

/** La respuesta guardada con la clave de la petición que la originó: si la clave ya no es la actual, es una respuesta vieja. */
interface RespuestaDeConsulta {
  clave: string;
  respuesta: Respuesta;
}

const ESPERA_ESCRITURA_MS = 400;

/**
 * Búsqueda de apartamentos disponibles (CU-02). Los filtros viven en el FiltrosProvider (useReducer), no en esta
 * página: al ir al detalle y volver siguen ahí. El formulario (React Hook Form + Zod) edita los filtros; cuando la
 * persona hace una pausa de escribir (useDebounce) se guardan en el proveedor y, si son válidos, se consulta al
 * servidor. Una consulta nueva cancela la anterior (AbortController), así una respuesta vieja nunca pisa a una nueva.
 */
export default function Buscar() {
  const { estado, dispatch } = useFiltros();
  const { sesion } = useAuth();

  const formulario = useForm<FormularioBusqueda>({
    resolver: zodResolver(esquemaBusqueda),
    mode: 'onChange',
    defaultValues: estado.filtros, // al volver del detalle el formulario nace con lo que la persona había escrito
  });
  const {
    register,
    control,
    formState: { errors },
  } = formulario;

  // Al abrir la página se valida todo el formulario: lo que falta se marca en su campo (is-invalid) desde el principio,
  // no solo cuando la persona toca cada uno.
  const { trigger } = formulario;
  useEffect(() => {
    void trigger();
  }, [trigger]);

  // ---------- Formulario → filtros (con pausa) ----------
  const valores = useWatch({ control });
  const valoresEstables = useDebounce(valores, ESPERA_ESCRITURA_MS);
  useEffect(() => {
    dispatch({ tipo: 'establecer', filtros: valoresEstables as FormularioBusqueda });
  }, [valoresEstables, dispatch]);

  // ---------- Catálogo de características (L-06): lo que se pinta es lo que responde el backend ----------
  const [catalogo, setCatalogo] = useState<Caracteristica[] | null>(null);
  const [errorCatalogo, setErrorCatalogo] = useState<string | null>(null);
  useEffect(() => {
    const controlador = new AbortController();
    listarCaracteristicas(controlador.signal)
      .then(setCatalogo)
      .catch((error: unknown) => {
        if (!fueCancelada(error)) setErrorCatalogo(mensajeDeError(error));
      });
    return () => controlador.abort();
  }, []);

  // ---------- Filtros válidos → consulta ----------
  const analisis = useMemo(() => esquemaBusqueda.safeParse(estado.filtros), [estado.filtros]);
  const solicitud = useMemo(() => (analisis.success ? solicitudDeBusqueda(analisis.data) : null), [analisis]);
  // Qué falta, con precisión ("Falta la fecha de nacimiento del ocupante 1"), para decírselo a la persona
  const faltantes = useMemo(() => (analisis.success ? [] : faltantesDeBusqueda(analisis.error, estado.filtros)), [analisis, estado.filtros]);

  // Un huésped escribe su fecha de nacimiento una vez: se recuerda (en esta pestaña) para las búsquedas siguientes
  const primero = estado.filtros.ocupantes[0];
  useEffect(() => {
    if (primero && sesion?.documento && primero.documento === sesion.documento) {
      recordarFechaNacimiento(primero.documento, primero.fechaNacimiento);
    }
  }, [primero, sesion?.documento]);

  // Mientras no haya una respuesta para la petición ACTUAL (clave distinta), se está cargando: se deriva, no se guarda.
  const clave = JSON.stringify([solicitud, estado.pagina]);
  const [guardada, setGuardada] = useState<RespuestaDeConsulta | null>(null);
  useEffect(() => {
    if (!solicitud) return;
    const controlador = new AbortController();
    consultarDisponibilidad(solicitud, estado.pagina, controlador.signal)
      .then((pagina) => setGuardada({ clave, respuesta: { tipo: 'resultado', pagina } }))
      .catch((error: unknown) => {
        if (fueCancelada(error)) return; // la cancelamos nosotros al cambiar los filtros: no es un error
        setGuardada({ clave, respuesta: { tipo: 'error', mensaje: mensajeDeError(error) } });
      });
    return () => controlador.abort();
  }, [solicitud, estado.pagina, clave]);
  const consulta = guardada?.clave === clave ? guardada.respuesta : null;

  return (
    <section>
      <h1 className="h3 mb-3">Buscar apartamento</h1>
      <div className="row g-4">
        <div className="col-12 col-lg-4">
          <FormProvider {...formulario}>
            <form noValidate onSubmit={(evento) => evento.preventDefault()} aria-label="Filtros de búsqueda">
              <div className="row">
                <div className="col-6">
                  <Campo
                    id="fechaEntrada"
                    type="date"
                    etiqueta="Entrada"
                    min={hoyISO()}
                    error={errors.fechaEntrada?.message}
                    {...register('fechaEntrada', { deps: ['fechaSalida'] })}
                  />
                </div>
                <div className="col-6">
                  <Campo
                    id="fechaSalida"
                    type="date"
                    etiqueta="Salida"
                    min={hoyISO()}
                    error={errors.fechaSalida?.message}
                    {...register('fechaSalida')}
                  />
                </div>
              </div>

              <CamposOcupantes />

              <fieldset className="mt-3">
                <legend className="h6">Filtros</legend>
                <Campo
                  id="capacidadMinima"
                  type="number"
                  min={1}
                  etiqueta="Capacidad mínima"
                  error={errors.capacidadMinima?.message}
                  {...register('capacidadMinima')}
                />
                <div className="row">
                  <div className="col-6">
                    <Campo
                      id="precioMinimo"
                      type="number"
                      min={0}
                      etiqueta="Precio mínimo"
                      error={errors.precioMinimo?.message}
                      {...register('precioMinimo')}
                    />
                  </div>
                  <div className="col-6">
                    <Campo
                      id="precioMaximo"
                      type="number"
                      min={0}
                      etiqueta="Precio máximo"
                      error={errors.precioMaximo?.message}
                      {...register('precioMaximo')}
                    />
                  </div>
                </div>
                <div className="form-text mb-2">El precio es el total de la estancia, en pesos.</div>

                <div className="mb-2">
                  <div className="form-label">Características (debe tener todas)</div>
                  {errorCatalogo && <div className="text-danger small">{errorCatalogo}</div>}
                  {!catalogo && !errorCatalogo && <div className="text-body-secondary small">Cargando características…</div>}
                  {catalogo?.map((caracteristica) => (
                    <div className="form-check" key={caracteristica}>
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id={`caracteristica-${caracteristica}`}
                        value={caracteristica}
                        {...register('caracteristicas')}
                      />
                      <label className="form-check-label" htmlFor={`caracteristica-${caracteristica}`}>
                        {etiquetarCaracteristica(caracteristica)}
                      </label>
                    </div>
                  ))}
                </div>
              </fieldset>
            </form>
          </FormProvider>
        </div>

        <div className="col-12 col-lg-8" aria-live="polite">
          {!solicitud ? (
            <div className="alert alert-info" role="status">
              <p className="mb-1">Para ver los apartamentos disponibles falta:</p>
              <ul className="mb-0">
                {faltantes.map((faltante) => (
                  <li key={faltante}>{faltante}</li>
                ))}
              </ul>
            </div>
          ) : consulta === null ? (
            <Cargando />
          ) : consulta.tipo === 'error' ? (
            // 409 (por ejemplo la estancia mínima de RP-01): el mensaje del dominio, no una página vacía
            <div className="alert alert-warning" role="alert">
              {consulta.mensaje}
            </div>
          ) : consulta.pagina.contenido.length === 0 ? (
            <div className="alert alert-secondary">No hay apartamentos disponibles con esos filtros para esas fechas.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {consulta.pagina.totalElementos}{' '}
                {consulta.pagina.totalElementos === 1 ? 'apartamento disponible' : 'apartamentos disponibles'}, del más económico al más costoso
              </p>
              {consulta.pagina.contenido.map((apartamento) => (
                <TarjetaApartamento key={apartamento.identificacion} apartamento={apartamento} />
              ))}
              <Paginacion
                paginaActual={consulta.pagina.pagina}
                totalPaginas={consulta.pagina.totalPaginas}
                alCambiar={(pagina) => dispatch({ tipo: 'irAPagina', pagina })}
              />
            </>
          )}
        </div>
      </div>
    </section>
  );
}
