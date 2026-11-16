import { Link } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { rutaInicial } from '../rutas/rutaInicial';
import { RUTAS } from '../rutas/rutas';

/** Se llega aquí cuando la persona ingresó, pero su rol no permite la página (o el backend respondió 403). */
export default function NoAutorizado() {
  const { sesion } = useAuth();

  return (
    <section className="text-center py-5">
      <h1 className="h3">No tiene permiso para ver esta página</h1>
      <p className="text-body-secondary">Su rol no permite esta sección o este recurso.</p>
      <Link to={sesion ? rutaInicial(sesion.roles) : RUTAS.login} className="btn btn-primary">
        {sesion ? 'Ir a mi página inicial' : 'Ingresar'}
      </Link>
    </section>
  );
}
