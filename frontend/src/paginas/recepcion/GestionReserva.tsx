import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { AccionesReservaRecepcion } from '../../componentes/AccionesReservaRecepcion';
import { Cargando } from '../../componentes/Cargando';
import { CierreFolio } from '../../componentes/CierreFolio';
import { DatosReserva } from '../../componentes/DatosReserva';
import { FolioDetalle } from '../../componentes/FolioDetalle';
import { FormularioModificarReserva } from '../../componentes/FormularioModificarReserva';
import { FormularioPago } from '../../componentes/FormularioPago';
import { useConsulta } from '../../hooks/useConsulta';
import type { AjusteFolioResponse } from '../../modelos/folio';
import { RUTAS } from '../../rutas/rutas';
import { obtenerFolio } from '../../servicios/folioServicio';
import { obtenerReserva } from '../../servicios/reservaServicio';
import { puedeModificar } from '../../utilidades/accionesReserva';
import { formatearPesos } from '../../utilidades/formato';

/**
 * Gestión de una reserva en recepción: datos, folio y todo lo que se puede hacer con ellos. Cada acción pasa por el
 * backend, que decide si es válida (409 con su mensaje); después de cada una se recargan la reserva y el folio.
 */
export default function GestionReserva() {
  const { codigo = '' } = useParams();
  const reserva = useConsulta(`reserva|${codigo}`, (signal) => obtenerReserva(codigo, signal));
  const folio = useConsulta(`folio|${codigo}`, (signal) => obtenerFolio(codigo, signal));
  const [ajustesRecientes, setAjustesRecientes] = useState<AjusteFolioResponse[]>([]);

  function recargarTodo() {
    reserva.recargar();
    folio.recargar();
  }

  if (reserva.resultado?.tipo === 'error') {
    return (
      <section>
        <h1 className="h3">Gestión de la reserva</h1>
        <div className="alert alert-danger" role="alert">
          {reserva.resultado.estadoHttp === 404 ? `No existe la reserva ${codigo}.` : reserva.resultado.mensaje}
        </div>
        <Link to={RUTAS.reservas} className="btn btn-outline-primary">Volver a las reservas</Link>
      </section>
    );
  }
  if (reserva.resultado === null) return <Cargando />;

  const datos = reserva.resultado.datos;
  const datosFolio = folio.resultado?.tipo === 'datos' ? folio.resultado.datos : null;

  return (
    <section>
      <h1 className="h3 mb-3">Gestión de la reserva</h1>

      <div className="row g-4">
        <div className="col-12 col-lg-7">
          <DatosReserva reserva={datos} />
        </div>

        <div className="col-12 col-lg-5">
          <div className="mb-4">
            {folio.cargando && <Cargando />}
            {folio.resultado?.tipo === 'error' && <div className="alert alert-warning" role="alert">{folio.resultado.mensaje}</div>}
            {datosFolio && <FolioDetalle folio={datosFolio} />}
            {ajustesRecientes.length > 0 && (
              <div className="alert alert-info" role="status">
                Ajuste del folio por la modificación:{' '}
                {ajustesRecientes.map((ajuste) => `${ajuste.concepto} (${formatearPesos(ajuste.valor)})`).join('; ')}
              </div>
            )}
          </div>

          <h3 className="h6">Acciones</h3>
          <div className="mb-4">
            <AccionesReservaRecepcion codigo={codigo} estado={datos.estado} alTerminar={recargarTodo} conEnlaceModificar={false} />
          </div>

          {datosFolio && !datosFolio.cerrado && (
            <div className="mb-4">
              <h3 className="h6">Registrar pago</h3>
              <FormularioPago
                codigo={codigo}
                alRegistrar={() => {
                  setAjustesRecientes([]);
                  folio.recargar();
                }}
              />
            </div>
          )}

          {datosFolio && !datosFolio.cerrado && (
            <div className="mb-4">
              <h3 className="h6">Cerrar folio</h3>
              <CierreFolio codigo={codigo} folio={datosFolio} alCerrar={recargarTodo} />
            </div>
          )}
        </div>
      </div>

      {puedeModificar(datos.estado) && (
        <div className="mt-4">
          <h2 className="h5">Modificar la reserva</h2>
          <p className="small text-body-secondary">
            Cambie las fechas, los ocupantes o el apartamento. El sistema vuelve a validar y recalcula el valor con las tarifas
            vigentes; la diferencia queda como ajuste en el folio.
          </p>
          {/* key: al recargar la reserva el formulario nace con los datos nuevos */}
          <FormularioModificarReserva
            key={`${datos.codigo}-${datos.historial.length}`}
            reserva={datos}
            ajustesAntes={datosFolio ? datosFolio.ajustes.length : null}
            alModificar={(ajustes) => {
              setAjustesRecientes(ajustes);
              recargarTodo();
            }}
          />
        </div>
      )}

      <div className="mt-4">
        <Link to={RUTAS.reservas} className="btn btn-outline-secondary">Volver a las reservas</Link>
      </div>
    </section>
  );
}
