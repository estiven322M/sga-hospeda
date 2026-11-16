import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useMemo, useState } from 'react';
import { FormProvider, useForm, useWatch } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router';
import { CamposOcupantes } from '../../componentes/CamposOcupantes';
import { Campo } from '../../componentes/Campo';
import { Cargando } from '../../componentes/Cargando';
import { OCUPANTE_VACIO, type FormularioOcupante } from '../../esquemas/ocupante';
import { edadALaFecha, esquemaReservaHuesped, type FormularioReservaHuesped } from '../../esquemas/reserva';
import { useAuth } from '../../hooks/useAuth';
import { useFiltros } from '../../hooks/useFiltros';
import type { ApartamentoResponse } from '../../modelos/apartamento';
import type { CrearReservaRequest } from '../../modelos/reserva';
import { RUTAS } from '../../rutas/rutas';
import { obtenerApartamento } from '../../servicios/apartamentoServicio';
import { crearReserva } from '../../servicios/reservaServicio';
import { erroresDeCampo, fueCancelada, mensajeDeError } from '../../utilidades/errores';
import { hoyISO } from '../../utilidades/fechas';
import { notificarExito } from '../../utilidades/notificaciones';

/**
 * Edad desde la que una persona cuenta como adulto responsable (RP-03). La regla y su umbral son del backend
 * (propiedad de configuración): este valor solo sirve para AVISAR antes de enviar; el que decide es el backend.
 */
const EDAD_ADULTO_RESPONSABLE = 18;

const CAMPOS_DEL_FORMULARIO = ['fechaEntrada', 'fechaSalida', 'horaEstimadaLlegada'] as const;

/**
 * Reserva del huésped por el PORTAL (CU-03), a su nombre: el titular es la persona de la sesión (su documento viaja en
 * el token y el backend lo exige igual). Parte de las fechas y los ocupantes de la búsqueda.
 */
export default function NuevaReservaHuesped() {
  const { identificacion = '' } = useParams();
  const navigate = useNavigate();
  const { sesion } = useAuth();
  const { estado } = useFiltros();

  const [apartamento, setApartamento] = useState<ApartamentoResponse | null>(null);
  const [errorApartamento, setErrorApartamento] = useState<string | null>(null);
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  useEffect(() => {
    const controlador = new AbortController();
    obtenerApartamento(identificacion, controlador.signal)
      .then(setApartamento)
      .catch((error: unknown) => {
        if (!fueCancelada(error)) setErrorApartamento(mensajeDeError(error));
      });
    return () => controlador.abort();
  }, [identificacion]);

  // Valores iniciales: el titular (sesión) primero, y detrás los demás ocupantes que ya se habían escrito en la búsqueda.
  const valoresIniciales = useMemo<FormularioReservaHuesped>(() => {
    const filtros = estado.filtros;
    const documento = sesion?.documento ?? '';
    const delFiltro = filtros.ocupantes.find((ocupante) => ocupante.documento === documento);
    const titular: FormularioOcupante = {
      documento,
      nombre: sesion?.nombre ?? delFiltro?.nombre ?? '',
      fechaNacimiento: delFiltro?.fechaNacimiento ?? '',
    };
    const demas = filtros.ocupantes.filter((ocupante) => ocupante.documento !== documento && ocupante.documento !== '');
    return {
      fechaEntrada: filtros.fechaEntrada,
      fechaSalida: filtros.fechaSalida,
      horaEstimadaLlegada: '',
      ocupantes: [titular, ...demas.map((ocupante) => ({ ...OCUPANTE_VACIO, ...ocupante }))],
    };
    // Solo al abrir la página: lo que se escriba después es del formulario, no de los filtros.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formulario = useForm<FormularioReservaHuesped>({
    resolver: zodResolver(esquemaReservaHuesped),
    mode: 'onChange',
    defaultValues: valoresIniciales,
  });
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = formulario;

  // ---------- Aviso de RP-03 (no bloquea): ningún ocupante será adulto a la fecha de entrada ----------
  const entrada = useWatch({ control, name: 'fechaEntrada' });
  const ocupantes = useWatch({ control, name: 'ocupantes' });
  const sinAdultoResponsable = useMemo(() => {
    const fechas = (ocupantes ?? []).map((ocupante) => ocupante.fechaNacimiento);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(entrada) || fechas.length === 0 || fechas.some((f) => !/^\d{4}-\d{2}-\d{2}$/.test(f))) {
      return false; // sin datos completos no se puede afirmar nada
    }
    return fechas.every((fecha) => edadALaFecha(fecha, entrada) < EDAD_ADULTO_RESPONSABLE);
  }, [entrada, ocupantes]);

  async function enviar(datos: FormularioReservaHuesped) {
    setErrorServidor(null);
    const [titular] = datos.ocupantes;
    if (!titular) return;
    const solicitud: CrearReservaRequest = {
      identificacionApartamento: identificacion,
      fechaEntrada: datos.fechaEntrada,
      fechaSalida: datos.fechaSalida,
      canalOrigen: 'PORTAL',
      ...(datos.horaEstimadaLlegada !== '' && { horaEstimadaLlegada: datos.horaEstimadaLlegada }),
      titular,
      ocupantes: datos.ocupantes,
    };
    let codigo: string;
    try {
      codigo = (await crearReserva(solicitud)).codigo;
    } catch (error) {
      // 409: el mensaje del dominio tal cual (RP-01, RP-03, disponibilidad...). 400: un mensaje por campo.
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS_DEL_FORMULARIO as readonly string[]).includes(campo)) {
          setError(campo as (typeof CAMPOS_DEL_FORMULARIO)[number], { message: mensaje });
        }
      }
      return;
    }
    // La reserva YA existe: lo que sigue no puede hacerse pasar por un error de la petición (la persona reintentaría
    // y crearía otra). Por eso el aviso y la navegación van fuera del try.
    notificarExito(`Reserva creada. Su código es ${codigo}`);
    void navigate(RUTAS.misReservas);
  }

  if (errorApartamento) {
    return (
      <section>
        <h1 className="h3">Nueva reserva</h1>
        <div className="alert alert-danger" role="alert">{errorApartamento}</div>
        <Link to={RUTAS.buscar} className="btn btn-outline-primary">Volver a la búsqueda</Link>
      </section>
    );
  }
  if (!apartamento) return <Cargando />;

  return (
    <section className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h3">Nueva reserva</h1>
        <p className="text-body-secondary">
          {apartamento.nombre} · capacidad {apartamento.capacidad} personas
        </p>

        {errorServidor && (
          <div className="alert alert-danger" role="alert">{errorServidor}</div>
        )}
        {sinAdultoResponsable && (
          <div className="alert alert-warning" role="status">
            Ningún ocupante tendrá {EDAD_ADULTO_RESPONSABLE} años cumplidos a la fecha de entrada. La reserva exige un adulto
            responsable (RP-03) y el sistema la rechazará si no lo hay.
          </div>
        )}

        <FormProvider {...formulario}>
          <form onSubmit={handleSubmit(enviar)} noValidate>
            <div className="row">
              <div className="col-12 col-md-4">
                <Campo
                  id="fechaEntrada"
                  type="date"
                  etiqueta="Entrada"
                  min={hoyISO()}
                  error={errors.fechaEntrada?.message}
                  {...register('fechaEntrada')}
                />
              </div>
              <div className="col-12 col-md-4">
                <Campo
                  id="fechaSalida"
                  type="date"
                  etiqueta="Salida"
                  min={hoyISO()}
                  error={errors.fechaSalida?.message}
                  {...register('fechaSalida')}
                />
              </div>
              <div className="col-12 col-md-4">
                <Campo
                  id="horaEstimadaLlegada"
                  type="time"
                  etiqueta="Hora estimada de llegada (opcional)"
                  error={errors.horaEstimadaLlegada?.message}
                  {...register('horaEstimadaLlegada')}
                />
              </div>
            </div>

            <CamposOcupantes documentoTitularFijo nombreTitularFijo={Boolean(sesion?.nombre)} />

            <div className="d-flex gap-2 mt-3">
              <button type="submit" className="btn btn-success" disabled={isSubmitting}>
                {isSubmitting ? 'Reservando…' : 'Reservar'}
              </button>
              <Link to={RUTAS.detalleApartamento(identificacion)} className="btn btn-outline-secondary">
                Volver al detalle
              </Link>
            </div>
          </form>
        </FormProvider>
      </div>
    </section>
  );
}
