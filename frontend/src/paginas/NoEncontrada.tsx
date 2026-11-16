import { Link } from 'react-router';
import { RUTAS } from '../rutas/rutas';

export default function NoEncontrada() {
  return (
    <section className="text-center py-5">
      <h1 className="h3">Página no encontrada</h1>
      <p className="text-body-secondary">La dirección que abrió no existe.</p>
      <Link to={RUTAS.inicio} className="btn btn-primary">
        Volver al inicio
      </Link>
    </section>
  );
}
