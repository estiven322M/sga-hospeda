import { Link } from 'react-router';
import { RUTAS } from '../rutas/rutas';

/** Página de inicio: presenta el alojamiento y lleva a lo que más se hace. Se carga de inmediato (sin lazy). */
export default function Inicio() {
  return (
    <section>
      <div className="hospeda-hero rounded-3 p-4 p-md-5 mb-4 text-white">
        <h1 className="display-6 fw-semibold mb-2">Hospeda</h1>
        <p className="lead mb-4">
          Apartamentos autónomos en Salento, Quindío. Consulte la disponibilidad para sus fechas, conozca
          el valor noche por noche y reserve en línea.
        </p>
        <div className="d-flex flex-wrap gap-2">
          <Link to={RUTAS.buscar} className="btn btn-light btn-lg">Buscar apartamento</Link>
          <Link to={RUTAS.misReservas} className="btn btn-outline-light btn-lg">Mis reservas</Link>
        </div>
      </div>

      {/* Grid: 1 columna en móvil, 3 desde md */}
      <div className="row g-3">
        <div className="col-12 col-md-4">
          <div className="card h-100">
            <div className="card-body">
              <h2 className="h5 card-title">Valor claro</h2>
              <p className="card-text">
                Cada noche se cobra con la tarifa de su temporada, por ocupante que paga. Desde 7 noches hay
                descuento por estancia larga.
              </p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-4">
          <div className="card h-100">
            <div className="card-body">
              <h2 className="h5 card-title">Apartamentos para cada grupo</h2>
              <p className="card-text">
                Filtre por capacidad, precio y características: terraza, balcón, vista, jacuzzi, parqueadero
                o acceso para personas con movilidad reducida.
              </p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-4">
          <div className="card h-100">
            <div className="card-body">
              <h2 className="h5 card-title">Reserva con respaldo</h2>
              <p className="card-text">
                Su reserva queda con el valor y la política de cancelación vigentes al momento de crearla.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
