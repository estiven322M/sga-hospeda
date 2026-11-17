import { useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Cargando } from '../../componentes/Cargando';
import { useConsulta } from '../../hooks/useConsulta';
import type { Resultado } from '../../hooks/useConsulta';
import type { FechaISO } from '../../modelos/comun';
import type { TipoReporte } from '../../modelos/reporte';
import { consultarIngresos, consultarOcupacion, consultarReservasPorCanal, descargarExcel } from '../../servicios/reporteServicio';
import { hoyISO, primerDiaDeMes } from '../../utilidades/fechas';
import { formatearFecha, formatearPesos } from '../../utilidades/formato';

interface Rango {
  desde: FechaISO;
  hasta: FechaISO;
}

/** Barra horizontal hecha solo con CSS: el ancho es el porcentaje (0 a 100) que ya viene calculado. */
function Barra({ porcentaje, etiqueta }: { porcentaje: number; etiqueta: string }) {
  const ancho = Math.max(0, Math.min(100, porcentaje));
  return (
    <div className="barra-reporte" role="img" aria-label={etiqueta}>
      <div className="barra-reporte__relleno" style={{ width: `${ancho}%` }} />
    </div>
  );
}

/** Parte de un total como porcentaje para dibujar la barra (solo presentación: las cifras son las del backend). */
function proporcion(parte: number, total: number): number {
  return total === 0 ? 0 : (parte * 100) / total;
}

interface TarjetaProps<T> {
  titulo: string;
  tipo: TipoReporte;
  rango: Rango;
  resultado: Resultado<T> | null;
  children: (datos: T) => ReactNode;
}

/** Marco común de cada reporte: título, botón "Descargar Excel" y los estados de carga y de error (409 con su mensaje). */
function TarjetaReporte<T>({ titulo, tipo, rango, resultado, children }: TarjetaProps<T>) {
  const [descargando, setDescargando] = useState(false);
  const [errorDescarga, setErrorDescarga] = useState<string | null>(null);

  async function descargar() {
    setDescargando(true);
    setErrorDescarga(null);
    try {
      await descargarExcel(tipo, rango.desde, rango.hasta);
    } catch {
      // La respuesta de error llega como Blob (no como ErrorResponse): se muestra un aviso y la pantalla sigue funcionando
      setErrorDescarga('No se pudo descargar el archivo de Excel. Intente de nuevo.');
    } finally {
      setDescargando(false);
    }
  }

  return (
    <article className="card mb-4">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
          <h2 className="h5 mb-0">{titulo}</h2>
          <button
            type="button"
            className="btn btn-outline-success btn-sm"
            onClick={descargar}
            disabled={descargando || resultado?.tipo !== 'datos'}
          >
            {descargando ? 'Descargando…' : 'Descargar Excel'}
          </button>
        </div>
        {errorDescarga && <div className="alert alert-warning py-2" role="alert">{errorDescarga}</div>}
        <div aria-live="polite">
          {resultado === null && <Cargando />}
          {resultado?.tipo === 'error' && <div className="alert alert-danger mb-0" role="alert">{resultado.mensaje}</div>}
          {resultado?.tipo === 'datos' && children(resultado.datos)}
        </div>
      </div>
    </article>
  );
}

/**
 * Reportes del alojamiento (7.9, L-19) con exportación a Excel (L-23), solo del administrador. El rango de fechas es
 * obligatorio; todas las cifras (noches vendidas, porcentaje, totales por medio de pago y por canal) las calcula el
 * backend: aquí solo se muestran en tablas y en barras de CSS. Un rango invertido responde 409 y se muestra su mensaje.
 */
export default function Reportes() {
  const hoy = hoyISO();
  const [formulario, setFormulario] = useState<Rango>({ desde: primerDiaDeMes(hoy, 0), hasta: hoy });
  const [rango, setRango] = useState<Rango>(formulario);

  const clave = `${rango.desde}|${rango.hasta}`;
  const ocupacion = useConsulta(clave, (signal) => consultarOcupacion(rango.desde, rango.hasta, signal));
  const ingresos = useConsulta(clave, (signal) => consultarIngresos(rango.desde, rango.hasta, signal));
  const canales = useConsulta(clave, (signal) => consultarReservasPorCanal(rango.desde, rango.hasta, signal));

  function consultar(evento: FormEvent) {
    evento.preventDefault();
    if (formulario.desde === rango.desde && formulario.hasta === rango.hasta) {
      ocupacion.recargar();
      ingresos.recargar();
      canales.recargar();
    } else {
      setRango(formulario);
    }
  }

  return (
    <section>
      <h1 className="h3">Reportes</h1>
      <p className="text-body-secondary">
        Ocupación, ingresos y reservas por canal de origen del periodo elegido (las dos fechas incluidas).
      </p>

      <form className="row g-3 align-items-end mb-4" onSubmit={consultar}>
        <div className="col-sm-4 col-md-3">
          <label htmlFor="reporte-desde" className="form-label">Desde</label>
          <input
            id="reporte-desde"
            type="date"
            className="form-control"
            required
            value={formulario.desde}
            onChange={(evento) => setFormulario({ ...formulario, desde: evento.target.value })}
          />
        </div>
        <div className="col-sm-4 col-md-3">
          <label htmlFor="reporte-hasta" className="form-label">Hasta</label>
          <input
            id="reporte-hasta"
            type="date"
            className="form-control"
            required
            value={formulario.hasta}
            onChange={(evento) => setFormulario({ ...formulario, hasta: evento.target.value })}
          />
        </div>
        <div className="col-sm-4 col-md-3">
          <button type="submit" className="btn btn-primary">Consultar</button>
        </div>
      </form>
      <p className="small text-body-secondary">
        Periodo consultado: {formatearFecha(rango.desde)} a {formatearFecha(rango.hasta)}
      </p>

      <TarjetaReporte titulo="Ocupación" tipo="ocupacion" rango={rango} resultado={ocupacion.resultado}>
        {(datos) => (
          <>
            <div className="d-flex align-items-center gap-3 mb-3">
              <strong className="fs-4">{datos.porcentajeOcupacion} %</strong>
              <div className="flex-grow-1">
                <Barra porcentaje={datos.porcentajeOcupacion} etiqueta={`Ocupación del ${datos.porcentajeOcupacion} %`} />
              </div>
            </div>
            <div className="table-responsive">
              <table className="table table-sm mb-0">
                <thead>
                  <tr>
                    <th>Apartamentos activos</th>
                    <th>Noches del periodo</th>
                    <th>Noches disponibles</th>
                    <th>Noches vendidas</th>
                    <th>Ocupación</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>{datos.apartamentosActivos}</td>
                    <td>{datos.nochesPeriodo}</td>
                    <td>{datos.nochesDisponibles}</td>
                    <td>{datos.nochesVendidas}</td>
                    <td>{datos.porcentajeOcupacion} %</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </>
        )}
      </TarjetaReporte>

      <TarjetaReporte titulo="Ingresos por medio de pago" tipo="ingresos" rango={rango} resultado={ingresos.resultado}>
        {(datos) =>
          datos.porMedio.length === 0 ? (
            <div className="alert alert-secondary mb-0">No se registraron pagos en el periodo.</div>
          ) : (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Medio de pago</th>
                    <th className="text-end">Total</th>
                    <th className="w-50">Participación</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.porMedio.map((fila) => (
                    <tr key={fila.medio}>
                      <td>{fila.medio}</td>
                      <td className="text-end">{formatearPesos(fila.total)}</td>
                      <td><Barra porcentaje={proporcion(fila.total, datos.total)} etiqueta={`Pagos por ${fila.medio}`} /></td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="fw-bold">
                    <td>Total</td>
                    <td className="text-end">{formatearPesos(datos.total)}</td>
                    <td />
                  </tr>
                </tfoot>
              </table>
            </div>
          )
        }
      </TarjetaReporte>

      <TarjetaReporte titulo="Reservas por canal de origen" tipo="reservas-por-canal" rango={rango} resultado={canales.resultado}>
        {(datos) => (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Canal</th>
                  <th className="text-end">Reservas</th>
                  <th className="text-end">Canceladas</th>
                  <th className="text-end">No-show</th>
                  <th className="w-50">Participación</th>
                </tr>
              </thead>
              <tbody>
                {datos.canales.map((fila) => (
                  <tr key={fila.canal}>
                    <td>{fila.canal}</td>
                    <td className="text-end">{fila.total}</td>
                    <td className="text-end">{fila.canceladas}</td>
                    <td className="text-end">{fila.noShow}</td>
                    <td><Barra porcentaje={proporcion(fila.total, datos.total)} etiqueta={`Reservas por ${fila.canal}`} /></td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="fw-bold">
                  <td>Total</td>
                  <td className="text-end">{datos.total}</td>
                  <td className="text-end">{datos.canceladas}</td>
                  <td className="text-end">{datos.noShow}</td>
                  <td />
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </TarjetaReporte>
    </section>
  );
}
