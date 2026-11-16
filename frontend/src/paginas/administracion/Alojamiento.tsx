import { Cargando } from '../../componentes/Cargando';
import { FormularioAlojamientoDatos } from '../../componentes/FormularioAlojamiento';
import { GestionServiciosAdicionales } from '../../componentes/GestionServiciosAdicionales';
import { TablaParametros } from '../../componentes/TablaParametros';
import { useConsulta } from '../../hooks/useConsulta';
import { obtenerAlojamiento, obtenerParametros } from '../../servicios/alojamientoServicio';

/**
 * Administración del alojamiento (7.3): sus datos editables (identidad, ubicación, horarios, normas y mascotas), los
 * parámetros del sistema en solo lectura y los servicios adicionales con su baja lógica.
 */
export default function Alojamiento() {
  const alojamiento = useConsulta('alojamiento', (signal) => obtenerAlojamiento(signal));
  const parametros = useConsulta('parametros', (signal) => obtenerParametros(signal));

  return (
    <section>
      <h1 className="h3 mb-3">Alojamiento</h1>

      <div className="row g-4">
        <div className="col-12 col-xl-7">
          <h2 className="h5">Datos del alojamiento</h2>
          {alojamiento.cargando && <Cargando />}
          {alojamiento.resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{alojamiento.resultado.mensaje}</div>}
          {alojamiento.resultado?.tipo === 'datos' && (
            <FormularioAlojamientoDatos alojamiento={alojamiento.resultado.datos} alGuardar={alojamiento.recargar} />
          )}
        </div>

        <div className="col-12 col-xl-5">
          <h2 className="h5">Parámetros del sistema</h2>
          {parametros.cargando && <Cargando />}
          {parametros.resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{parametros.resultado.mensaje}</div>}
          {parametros.resultado?.tipo === 'datos' && <TablaParametros parametros={parametros.resultado.datos} />}
        </div>
      </div>

      <h2 className="h5 mt-5">Servicios adicionales</h2>
      <GestionServiciosAdicionales />
    </section>
  );
}
