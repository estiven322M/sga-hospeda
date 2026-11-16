import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import type { RolUsuario } from '../modelos/auth';
import { RUTAS } from './rutas';

interface RutaProtegidaProps {
  /** Roles que pueden entrar. Si no se indica, basta con haber ingresado. */
  roles?: readonly RolUsuario[];
}

/**
 * Ruta de diseño que protege a todas sus rutas hijas. Es una ayuda de navegación, NO la seguridad del sistema:
 * quien decide qué puede hacer cada rol es el backend (Guía 12). Esto solo evita mostrar pantallas que de todas
 * formas responderían 401 o 403.
 */
export function RutaProtegida({ roles }: RutaProtegidaProps) {
  const { autenticado, tieneRol } = useAuth();
  const ubicacion = useLocation();

  if (!autenticado) {
    // replace: la página bloqueada no queda en el historial ("Atrás" no vuelve a ella).
    // state: el login sabrá a dónde volver después de ingresar.
    return <Navigate to={RUTAS.login} replace state={{ desde: ubicacion.pathname }} />;
  }
  if (roles && !tieneRol(...roles)) {
    return <Navigate to={RUTAS.noAutorizado} replace />;
  }
  return <Outlet />;
}
