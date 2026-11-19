import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { esquemaTarifa } from '../esquemas/administracion';
import { useConsulta } from '../hooks/useConsulta';
import type { TarifaResponse, TemporadaResponse } from '../modelos/tarifario';
import { listarTodasLasTarifas, registrarTarifa } from '../servicios/temporadaServicio';
import { mensajeDeError } from '../utilidades/errores';
import { formatearPesos } from '../utilidades/formato';
import { notificarExito } from '../utilidades/notificaciones';
import { Campo } from './Campo';
import { Cargando } from './Cargando';
import { PeriodosTemporada } from './PeriodosTemporada';

interface FilaTarifaProps {
  identificacion: string;
  temporada: TemporadaResponse;
  tarifa: TarifaResponse | undefined;
  alGuardar: () => void;
}

/** Una temporada con su tarifa actual (o la marca "Sin tarifa") y el campo para definirla o reemplazarla. */
function FilaTarifa({ identificacion, temporada, tarifa, alGuardar }: FilaTarifaProps) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<{ valor: string }>({
    resolver: zodResolver(esquemaTarifa),
    mode: 'onChange',
    defaultValues: { valor: '' },
  });

  async function enviar(datos: { valor: string }) {
    setErrorServidor(null);
    try {
      await registrarTarifa(identificacion, temporada.nombre, { valorPorOcupanteNoche: Number(datos.valor) });
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      return;
    }
    notificarExito(`Tarifa de ${temporada.nombre} registrada`);
    reset({ valor: '' });
    alGuardar();
  }

  return (
    <article className={`card mb-2 fila-tarifa ${tarifa ? '' : 'border-warning'}`}>
      <div className="card-body">
        <div className="row align-items-center g-2">
          <div className="col-12 col-md-4">
            <div className="fw-semibold">
              {temporada.nombre} {temporada.base && <span className="badge text-bg-light border">Base</span>}
            </div>
            <PeriodosTemporada periodos={temporada.periodos} textoSinPeriodos="Resto del año" />
          </div>
          <div className="col-12 col-md-3">
            {tarifa ? (
              <span>{formatearPesos(tarifa.valorPorOcupanteNoche)} <span className="small text-body-secondary">por ocupante y noche</span></span>
            ) : (
              <span className="badge text-bg-warning">Sin tarifa</span>
            )}
          </div>
          <form className="col-12 col-md-5" onSubmit={handleSubmit(enviar)} noValidate aria-label={`Tarifa de ${temporada.nombre}`}>
            <div className="d-flex gap-2 align-items-start">
              <div className="flex-grow-1">
                <Campo
                  id={`tarifa-${temporada.nombre}`}
                  type="number"
                  min={1}
                  etiqueta={tarifa ? 'Nuevo valor' : 'Valor por ocupante y noche'}
                  error={errors.valor?.message}
                  {...register('valor')}
                />
              </div>
              <button type="submit" className="btn btn-sm btn-primary mt-4" disabled={isSubmitting}>
                {tarifa ? 'Reemplazar' : 'Registrar'}
              </button>
            </div>
            {errorServidor && <div className="text-danger small">{errorServidor}</div>}
          </form>
        </div>
      </div>
    </article>
  );
}

/**
 * Las tarifas de un apartamento en CADA temporada. Las temporadas sin tarifa se marcan: mientras falte alguna el
 * apartamento no se puede activar (7.4; el backend responde 409).
 */
export function PanelTarifas({ identificacion, temporadas }: { identificacion: string; temporadas: TemporadaResponse[] }) {
  const consulta = useConsulta(`tarifas|${identificacion}`, (signal) => listarTodasLasTarifas(identificacion, signal));
  const { recargar } = consulta;
  // Mientras se recarga tras guardar una tarifa se siguen mostrando las filas (con lo que haya escrito en las demás)
  const resultado = consulta.resultado ?? consulta.ultimo;

  if (resultado === null) return <Cargando />;
  if (resultado.tipo === 'error') return <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>;

  const tarifas = new Map(resultado.datos.map((tarifa) => [tarifa.temporada, tarifa]));
  const faltantes = temporadas.filter((temporada) => !tarifas.has(temporada.nombre));

  return (
    <div>
      {faltantes.length > 0 ? (
        <div className="alert alert-warning" role="status">
          Faltan tarifas en {faltantes.length} {faltantes.length === 1 ? 'temporada' : 'temporadas'}: {faltantes.map((t) => t.nombre).join(', ')}. Hasta
          completarlas el apartamento no se puede activar.
        </div>
      ) : (
        <div className="alert alert-success" role="status">Tiene tarifa en todas las temporadas.</div>
      )}
      {temporadas.map((temporada) => (
        <FilaTarifa key={temporada.nombre} identificacion={identificacion} temporada={temporada} tarifa={tarifas.get(temporada.nombre)} alGuardar={recargar} />
      ))}
    </div>
  );
}
