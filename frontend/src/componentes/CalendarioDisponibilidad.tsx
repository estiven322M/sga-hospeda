import { useConsulta } from '../hooks/useConsulta';
import { obtenerCalendario } from '../servicios/calendarioServicio';
import { hoyISO, primerDiaDeMes } from '../utilidades/fechas';
import { Cargando } from './Cargando';
import { GrillaCalendario } from './GrillaCalendario';

/**
 * Calendario de disponibilidad de un apartamento: desde hoy hasta el final del mes siguiente (a lo sumo 62 noches, el
 * máximo que admite el backend). El huésped ve LIBRE o NO_DISPONIBLE; recepción y administración ven además por qué.
 * Si el calendario no carga, la página que lo contiene sigue funcionando (solo se avisa).
 */
export function CalendarioDisponibilidad({ identificacion }: { identificacion: string }) {
  const desde = hoyISO();
  const hasta = primerDiaDeMes(desde, 2);
  const { resultado, cargando } = useConsulta(`calendario|${identificacion}|${desde}`, (signal) =>
    obtenerCalendario(identificacion, desde, hasta, signal),
  );

  return (
    <div aria-live="polite">
      {cargando && <Cargando />}
      {resultado?.tipo === 'error' && (
        <div className="alert alert-warning" role="status">No se pudo cargar el calendario: {resultado.mensaje}</div>
      )}
      {resultado?.tipo === 'datos' && <GrillaCalendario noches={resultado.datos.noches} />}
    </div>
  );
}
