import { Link } from 'react-router';
import { InformacionAlojamiento } from '../componentes/InformacionAlojamiento';
import { RUTAS } from '../rutas/rutas';

/** Los tres pasos para reservar, en el orden en que los vive el huésped. */
const PASOS = [
  { titulo: 'Elija fechas y grupo', texto: 'Indique cuándo llega, cuándo sale y quiénes viajan.' },
  { titulo: 'Compare apartamentos', texto: 'Vea capacidad, características y el valor total de la estancia.' },
  { titulo: 'Reserve en línea', texto: 'Su reserva queda con el valor y la política vigentes al crearla.' },
] as const;

const ICONO_PROPIEDADES = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
} as const;

/** Página de inicio: presenta el alojamiento y lleva a lo que más se hace. Se carga de inmediato (sin lazy). */
export default function Inicio() {
  return (
    <section>
      {/* Portada: foto del Valle del Cocora de fondo, con una capa verde a la izquierda para que el texto se lea */}
      <div className="hospeda-hero rounded-3 p-4 p-md-5 mb-5 text-white">
        <div className="row align-items-center g-4">
          <div className="col-12 col-lg-7 col-xl-6">
            <span className="badge text-bg-light text-success mb-3">Salento · Quindío · Eje Cafetero</span>
            <h1 className="display-5 fw-bold mb-1">Sistema de Gestión de Alojamiento</h1>
            <p className="hospeda-subtitulo mb-3">Hospeda</p>
            <p className="lead mb-4">
              Apartamentos autónomos en Salento, Quindío. Consulte la disponibilidad para sus fechas, conozca
              el valor noche por noche y reserve en línea.
            </p>
            <div className="d-flex flex-wrap gap-2">
              <Link to={RUTAS.buscar} className="btn btn-acento btn-lg px-4">Buscar apartamento</Link>
              <Link to={RUTAS.misReservas} className="btn btn-outline-light btn-lg">Mis reservas</Link>
            </div>
          </div>
        </div>
        <span className="hospeda-hero__lugar">Valle del Cocora, Salento</span>
      </div>

      {/* Cómo funciona */}
      <h2 className="seccion-titulo">Reservar es simple</h2>
      <ol className="row g-3 list-unstyled mb-5">
        {PASOS.map((paso, indice) => (
          <li key={paso.titulo} className="col-12 col-md-4">
            <div className="paso h-100">
              <span className="paso__numero">{indice + 1}</span>
              <h3 className="h6 mb-1">{paso.titulo}</h3>
              <p className="mb-0 text-body-secondary small">{paso.texto}</p>
            </div>
          </li>
        ))}
      </ol>

      {/* Ventajas. Grid: 1 columna en móvil, 3 desde md */}
      <h2 className="seccion-titulo">Por qué Hospeda</h2>
      <div className="row g-3">
        <div className="col-12 col-md-4">
          <div className="card h-100 tarjeta-elevable">
            <div className="card-body">
              <span className="hospeda-ventaja">
                <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" {...ICONO_PROPIEDADES}>
                  <path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
                </svg>
              </span>
              <h3 className="h5 card-title">Valor claro</h3>
              <p className="card-text">
                Cada noche se cobra con la tarifa de su temporada, por ocupante que paga. Desde 7 noches hay
                descuento por estancia larga.
              </p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-4">
          <div className="card h-100 tarjeta-elevable">
            <div className="card-body">
              <span className="hospeda-ventaja">
                <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" {...ICONO_PROPIEDADES}>
                  <path d="M3 21V8l9-5 9 5v13M9 21v-6h6v6" />
                </svg>
              </span>
              <h3 className="h5 card-title">Apartamentos para cada grupo</h3>
              <p className="card-text">
                Filtre por capacidad, precio y características: terraza, balcón, vista, jacuzzi, parqueadero
                o acceso para personas con movilidad reducida.
              </p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-4">
          <div className="card h-100 tarjeta-elevable">
            <div className="card-body">
              <span className="hospeda-ventaja">
                <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" {...ICONO_PROPIEDADES}>
                  <path d="M12 2 4 5v6c0 5 3.4 9.3 8 11 4.6-1.7 8-6 8-11V5zM9 12l2 2 4-4" />
                </svg>
              </span>
              <h3 className="h5 card-title">Reserva con respaldo</h3>
              <p className="card-text">
                Su reserva queda con el valor y la política de cancelación vigentes al momento de crearla.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Horarios, normas de convivencia y mascotas: vienen de GET /alojamiento (público); si no cargan, no se muestra nada */}
      <div className="mt-5">
        <InformacionAlojamiento />
      </div>

      {/* Llamado final, con el logo del sistema */}
      <div className="llamado-final mt-5">
        <div className="row align-items-center g-4">
          <div className="col-12 col-md-5 text-center">
            <img src="/logo-sga.png" alt="Logo de SGA, Sistema de Gestión de Alojamiento" className="llamado-final__logo" />
          </div>
          <div className="col-12 col-md-7 text-center text-md-start">
            <h2 className="h3 mb-2">¿Listo para su estancia en Salento?</h2>
            <p className="mb-4 text-body-secondary">
              Consulte la disponibilidad y conozca el valor de sus fechas antes de reservar.
            </p>
            <Link to={RUTAS.buscar} className="btn btn-acento btn-lg px-4">Buscar apartamento</Link>
          </div>
        </div>
      </div>
    </section>
  );
}
