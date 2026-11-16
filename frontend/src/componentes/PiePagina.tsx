// Se calcula una sola vez, al cargar el módulo: un componente debe ser puro y no consultar el reloj en cada render.
const ANIO_ACTUAL = new Date().getFullYear();

export function PiePagina() {
  return (
    <footer className="bg-body-tertiary border-top py-3 mt-4">
      <div className="container text-center text-body-secondary small">
        <div>© {ANIO_ACTUAL} Hospeda — Salento, Quindío</div>
        <div>Universidad del Quindío · Programación Avanzada</div>
      </div>
    </footer>
  );
}
