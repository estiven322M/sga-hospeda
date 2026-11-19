// Se calcula una sola vez, al cargar el módulo: un componente debe ser puro y no consultar el reloj en cada render.
const ANIO_ACTUAL = new Date().getFullYear();

export function PiePagina() {
  return (
    <footer className="hospeda-pie py-5 mt-5">
      <div className="container">
        <div className="row g-4">
          <div className="col-12 col-md-5">
            <div className="hospeda-pie__marca">Hospeda</div>
            <p className="small mb-0">
              Sistema de Gestión de Alojamiento. Apartamentos autónomos en Salento, Quindío.
            </p>
          </div>
          <div className="col-6 col-md-3 offset-md-1">
            <div className="hospeda-pie__titulo">Ubicación</div>
            <p className="small mb-0">Salento, Quindío<br />Colombia</p>
          </div>
          <div className="col-6 col-md-3">
            <div className="hospeda-pie__titulo">Proyecto</div>
            <p className="small mb-0">Universidad del Quindío<br />Programación Avanzada</p>
          </div>
        </div>
        <hr className="hospeda-pie__linea" />
        <div className="d-flex flex-column flex-md-row justify-content-between gap-2 small">
          <span>© {ANIO_ACTUAL} Hospeda — Salento, Quindío</span>
          {/* Las fotos son de Unsplash (uso libre); se da el crédito a sus autores */}
          <span className="hospeda-pie__creditos">Fotos: Shawn Appel y Juan Manuel Núñez Méndez en Unsplash</span>
        </div>
      </div>
    </footer>
  );
}
