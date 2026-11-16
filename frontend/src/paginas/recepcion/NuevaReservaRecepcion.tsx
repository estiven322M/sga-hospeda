import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { AvisoAdultoResponsable } from '../../componentes/AvisoAdultoResponsable';
import { CamposOcupantes } from '../../componentes/CamposOcupantes';
import { Campo } from '../../componentes/Campo';
import { CampoSelect } from '../../componentes/CampoSelect';
import { OCUPANTE_VACIO } from '../../esquemas/ocupante';
import { esquemaReservaRecepcion, type FormularioReservaRecepcion } from '../../esquemas/reserva';
import { useApartamentos } from '../../hooks/useApartamentos';
import type { CrearReservaRequest } from '../../modelos/reserva';
import { RUTAS } from '../../rutas/rutas';
import { crearReserva } from '../../servicios/reservaServicio';
import { erroresDeCampo, mensajeDeError } from '../../utilidades/errores';
import { hoyISO } from '../../utilidades/fechas';
import { notificarExito } from '../../utilidades/notificaciones';

const CAMPOS_DEL_FORMULARIO = ['identificacionApartamento', 'fechaEntrada', 'fechaSalida', 'horaEstimadaLlegada', 'correoTitular'] as const;

/**
 * Reserva de recepción por el canal DIRECTO (CU-03): la persona de recepción escribe el titular, los ocupantes, las
 * fechas, el apartamento y la hora estimada. El primer ocupante es el titular. Zod revisa la forma; el negocio (estancia
 * mínima, adulto responsable, disponibilidad...) lo decide el backend y sus 409 se muestran tal cual.
 */
export default function NuevaReservaRecepcion() {
  const navigate = useNavigate();
  const { apartamentos, error: errorApartamentos } = useApartamentos();
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  const formulario = useForm<FormularioReservaRecepcion>({
    resolver: zodResolver(esquemaReservaRecepcion),
    mode: 'onChange',
    defaultValues: {
      identificacionApartamento: '',
      fechaEntrada: '',
      fechaSalida: '',
      horaEstimadaLlegada: '',
      correoTitular: '',
      ocupantes: [{ ...OCUPANTE_VACIO }],
    },
  });
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = formulario;

  async function enviar(datos: FormularioReservaRecepcion) {
    setErrorServidor(null);
    const [titular] = datos.ocupantes;
    if (!titular) return;
    const solicitud: CrearReservaRequest = {
      identificacionApartamento: datos.identificacionApartamento,
      fechaEntrada: datos.fechaEntrada,
      fechaSalida: datos.fechaSalida,
      canalOrigen: 'DIRECTO',
      ...(datos.horaEstimadaLlegada !== '' && { horaEstimadaLlegada: datos.horaEstimadaLlegada }),
      ...(datos.correoTitular !== '' && { correoTitular: datos.correoTitular }),
      titular,
      ocupantes: datos.ocupantes,
    };
    let codigo: string;
    try {
      codigo = (await crearReserva(solicitud)).codigo;
    } catch (error) {
      // 409: el mensaje del dominio tal cual. 400: un mensaje por campo.
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS_DEL_FORMULARIO as readonly string[]).includes(campo)) {
          setError(campo as (typeof CAMPOS_DEL_FORMULARIO)[number], { message: mensaje });
        }
      }
      return;
    }
    // La reserva ya existe: el aviso y la navegación van fuera del try para no confundirla con un error de la petición.
    notificarExito(`Reserva creada. Su código es ${codigo}`);
    void navigate(RUTAS.gestionReserva(codigo));
  }

  return (
    <section className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <h1 className="h3 mb-3">Nueva reserva (recepción)</h1>

        {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
        {errorApartamentos && <div className="alert alert-danger" role="alert">{errorApartamentos}</div>}

        <FormProvider {...formulario}>
          <form onSubmit={handleSubmit(enviar)} noValidate>
            <CampoSelect id="identificacionApartamento" etiqueta="Apartamento" error={errors.identificacionApartamento?.message} {...register('identificacionApartamento')}>
              <option value="">Elija un apartamento</option>
              {apartamentos
                ?.filter((apartamento) => apartamento.activo)
                .map((apartamento) => (
                  <option key={apartamento.identificacion} value={apartamento.identificacion}>
                    {apartamento.identificacion} · {apartamento.nombre} (capacidad {apartamento.capacidad})
                  </option>
                ))}
            </CampoSelect>

            <div className="row">
              <div className="col-12 col-md-4">
                <Campo id="fechaEntrada" type="date" etiqueta="Entrada" min={hoyISO()} error={errors.fechaEntrada?.message} {...register('fechaEntrada', { deps: ['fechaSalida'] })} />
              </div>
              <div className="col-12 col-md-4">
                <Campo id="fechaSalida" type="date" etiqueta="Salida" min={hoyISO()} error={errors.fechaSalida?.message} {...register('fechaSalida')} />
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

            <AvisoAdultoResponsable />
            <CamposOcupantes primerEsTitular />

            <Campo
              id="correoTitular"
              type="email"
              etiqueta="Correo del titular (opcional)"
              autoComplete="off"
              error={errors.correoTitular?.message}
              {...register('correoTitular')}
            />
            <div className="form-text mb-3">
              Se le avisará a este correo cuando la reserva se cree, se confirme o se cancele. Si el titular ya tiene cuenta, se usa el correo de su cuenta.
            </div>

            <div className="d-flex gap-2 mt-3">
              <button type="submit" className="btn btn-success" disabled={isSubmitting}>
                {isSubmitting ? 'Creando…' : 'Crear reserva'}
              </button>
              <Link to={RUTAS.reservas} className="btn btn-outline-secondary">Volver a las reservas</Link>
            </div>
          </form>
        </FormProvider>
      </div>
    </section>
  );
}
