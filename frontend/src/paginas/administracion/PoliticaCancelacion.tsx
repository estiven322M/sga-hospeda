import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { Campo } from '../../componentes/Campo';
import { Cargando } from '../../componentes/Cargando';
import { TablaTramos } from '../../componentes/TablaTramos';
import { esquemaPolitica, solicitudDePolitica, type FormularioPolitica } from '../../esquemas/administracion';
import { useConsulta } from '../../hooks/useConsulta';
import type { PoliticaCancelacionResponse } from '../../modelos/tarifario';
import { obtenerPoliticaPorVersion, obtenerPoliticaVigente, registrarPoliticaCancelacion } from '../../servicios/politicaServicio';
import { mensajeDeError } from '../../utilidades/errores';
import { notificarExito } from '../../utilidades/notificaciones';

/**
 * Política de cancelación (L-10), versionada: se ve la vigente, se consulta cualquier versión anterior y se registra
 * una nueva (el sistema le asigna el número siguiente). Las reservas ya creadas conservan la versión con la que se
 * crearon (RN-13): publicar una nueva solo afecta a las reservas que se creen desde entonces.
 */
export default function PoliticaCancelacion() {
  const vigente = useConsulta('vigente', (signal) => obtenerPoliticaVigente(signal));
  const [versionPedida, setVersionPedida] = useState<number | null>(null);
  const [textoVersion, setTextoVersion] = useState('');
  const consultada = useConsulta(versionPedida === null ? null : `version|${versionPedida}`, (signal) => obtenerPoliticaPorVersion(versionPedida ?? 0, signal));
  const [registrada, setRegistrada] = useState<PoliticaCancelacionResponse | null>(null);
  const [errorServidor, setErrorServidor] = useState<string | null>(null);

  const errorVersion = textoVersion !== '' && !/^[1-9]\d*$/.test(textoVersion) ? 'La versión es un número entero desde 1' : undefined;

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormularioPolitica>({
    resolver: zodResolver(esquemaPolitica),
    mode: 'onChange',
    defaultValues: {
      vigenteDesde: '',
      tramos: [
        { horasAntelacionMinima: '', porcentajeRetencion: '' },
        { horasAntelacionMinima: '0', porcentajeRetencion: '' },
      ],
    },
  });
  const tramos = useFieldArray({ control, name: 'tramos' });

  async function enviar(datos: FormularioPolitica) {
    setErrorServidor(null);
    setRegistrada(null);
    try {
      setRegistrada(await registrarPoliticaCancelacion(solicitudDePolitica(datos)));
    } catch (error) {
      setErrorServidor(mensajeDeError(error)); // 409: la regla del dominio, tal cual
      return;
    }
    notificarExito('Nueva versión de la política registrada');
    reset();
    vigente.recargar();
  }

  return (
    <section>
      <h1 className="h3 mb-3">Política de cancelación</h1>
      <div className="alert alert-info" role="note">
        Las reservas existentes conservan la política con la que se crearon. Una versión nueva solo se aplica a las reservas
        que se creen desde su fecha de vigencia.
      </div>

      <div className="row g-4">
        <div className="col-12 col-lg-6">
          <h2 className="h5">Vigente</h2>
          {vigente.cargando && <Cargando />}
          {vigente.resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{vigente.resultado.mensaje}</div>}
          {vigente.resultado?.tipo === 'datos' && <TablaTramos politica={vigente.resultado.datos} />}

          <h2 className="h5 mt-4">Consultar una versión</h2>
          <form
            className="d-flex gap-2 align-items-start"
            noValidate
            onSubmit={(evento) => {
              evento.preventDefault();
              if (textoVersion !== '' && !errorVersion) setVersionPedida(Number(textoVersion));
            }}
            aria-label="Consultar una versión"
          >
            <div className="flex-grow-1">
              <Campo id="version" type="number" min={1} etiqueta="Número de versión" value={textoVersion} onChange={(evento) => setTextoVersion(evento.target.value)} error={errorVersion} />
            </div>
            <button type="submit" className="btn btn-outline-primary mt-4">Consultar</button>
          </form>
          {consultada.cargando && <Cargando />}
          {consultada.resultado?.tipo === 'error' && (
            <div className="alert alert-warning" role="alert">
              {consultada.resultado.estadoHttp === 404 ? `No existe la versión ${versionPedida} de la política.` : consultada.resultado.mensaje}
            </div>
          )}
          {consultada.resultado?.tipo === 'datos' && <TablaTramos politica={consultada.resultado.datos} />}
        </div>

        <div className="col-12 col-lg-6">
          <h2 className="h5">Registrar una nueva versión</h2>
          {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
          {registrada && (
            <div className="alert alert-success" role="status">
              Se registró la versión {registrada.version}, que rige desde el {registrada.vigenteDesde}.
            </div>
          )}
          <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Registrar una política">
            <Campo id="vigenteDesde" type="date" etiqueta="Rige desde" error={errors.vigenteDesde?.message} {...register('vigenteDesde')} />
            <fieldset className="mb-3">
              <legend className="form-label fs-6">Tramos de retención</legend>
              {tramos.fields.map((campo, indice) => (
                <div key={campo.id} className="row align-items-start">
                  <div className="col-5">
                    <Campo
                      id={`tramo-${indice}-horas`}
                      type="number"
                      min={0}
                      etiqueta="Antelación mínima (horas)"
                      error={errors.tramos?.[indice]?.horasAntelacionMinima?.message}
                      {...register(`tramos.${indice}.horasAntelacionMinima`)}
                    />
                  </div>
                  <div className="col-5">
                    <Campo
                      id={`tramo-${indice}-porcentaje`}
                      type="number"
                      min={0}
                      max={100}
                      etiqueta="Retención (%)"
                      error={errors.tramos?.[indice]?.porcentajeRetencion?.message}
                      {...register(`tramos.${indice}.porcentajeRetencion`)}
                    />
                  </div>
                  <div className="col-2">
                    {tramos.fields.length > 2 && (
                      <button type="button" className="btn btn-sm btn-outline-danger mt-4" onClick={() => tramos.remove(indice)}>
                        Quitar
                      </button>
                    )}
                  </div>
                </div>
              ))}
              {errors.tramos?.message && <div className="text-danger small mb-2">{errors.tramos.message}</div>}
              {errors.tramos?.root?.message && <div className="text-danger small mb-2">{errors.tramos.root.message}</div>}
              <div className="form-text mb-2">Debe haber un tramo de antelación 0: es el que se aplica a lo más tardío.</div>
              <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => tramos.append({ horasAntelacionMinima: '', porcentajeRetencion: '' })}>
                Agregar tramo
              </button>
            </fieldset>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Registrando…' : 'Registrar política'}
            </button>
          </form>
        </div>
      </div>
    </section>
  );
}
