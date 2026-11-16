import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { puedeAcceder } from './acceso';
import { rutaInicial } from './rutaInicial';

/**
 * Rutas solo para quien NO ha ingresado (login y registro): lógica inversa a RutaProtegida. También decide a dónde
 * ir al ingresar: en cuanto la sesión existe este componente se vuelve a pintar y redirige, así que el login no
 * necesita navegar por su cuenta.
 */
export function RutaPublica() {
  const { sesion } = useAuth();
  const ubicacion = useLocation();

  if (sesion) {
    // Si RutaProtegida envió a la persona al login, se vuelve a la página que intentaba abrir, pero solo si su rol
    // puede verla; si no, a la página inicial de su rol.
    const desde = (ubicacion.state as { desde?: string } | null)?.desde;
    const destino = desde && puedeAcceder(desde, sesion.roles) ? desde : rutaInicial(sesion.roles);
    return <Navigate to={destino} replace />;
  }
  return <Outlet />;
}
