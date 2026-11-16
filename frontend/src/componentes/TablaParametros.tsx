import type { ParametrosResponse } from '../modelos/alojamiento';
import { formatearHora } from '../utilidades/formato';

/**
 * Los parámetros de la sección 6 en SOLO LECTURA. Son configuración del sistema (application.properties o variables de
 * entorno): aquí no se editan, y la nota que entrega el backend lo aclara. Solo presentación.
 */
export function TablaParametros({ parametros }: { parametros: ParametrosResponse }) {
  const minimos = Object.entries(parametros.estanciaMinimaPorTemporada);
  const filas: [string, string][] = [
    ['Umbral de edad facturable', `${parametros.umbralEdadFacturableAnios} años`],
    ['Tiempo de preparación entre estancias', `${parametros.tiempoPreparacionHoras} horas`],
    ['Plazo de confirmación de una reserva pendiente', `${parametros.plazoConfirmacionHoras} horas`],
    ['Hora límite para declarar no-show', formatearHora(parametros.horaLimiteNoShow)],
    ['Anticipo para confirmar', `${parametros.anticipoPorcentaje} % del valor`],
    [
      'Estancia mínima por temporada (RP-01)',
      minimos.length === 0 ? 'Ninguna temporada exige mínimo' : minimos.map(([temporada, noches]) => `${temporada}: ${noches} noches`).join(' · '),
    ],
    ['Descuento por estancia larga (RP-02)', `${parametros.porcentajeDescuentoEstanciaLarga} % desde ${parametros.nochesDescuentoEstanciaLarga} noches`],
    ['Edad del adulto responsable (RP-03)', `${parametros.edadAdultoResponsableAnios} años`],
    ['Medios de pago aceptados', parametros.mediosPago.join(', ')],
    ['Máximo de noches del calendario', `${parametros.calendarioMaximoNoches} noches`],
    ['Zona horaria', parametros.zonaHoraria],
  ];

  return (
    <div>
      <div className="alert alert-secondary py-2 small" role="note">{parametros.nota}</div>
      <div className="table-responsive">
        <table className="table table-sm align-middle">
          <tbody>
            {filas.map(([parametro, valor]) => (
              <tr key={parametro}>
                <th scope="row" className="fw-normal text-body-secondary">{parametro}</th>
                <td>{valor}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
