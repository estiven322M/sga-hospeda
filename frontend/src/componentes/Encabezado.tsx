import { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router';
import { useAuth } from '../hooks/useAuth';
import { establecerFotoDePerfil, useFotoPerfil } from '../hooks/useFotoPerfil';
import type { RolUsuario } from '../modelos/auth';
import { RUTAS } from '../rutas/rutas';
import { obtenerMiPerfil } from '../servicios/usuarioServicio';
import { Avatar } from './Avatar';
import { MenuDesplegable } from './MenuDesplegable';

interface EncabezadoProps {
  nombreAlojamiento: string;
}

const OPCIONES_RECEPCION = [
  { etiqueta: 'Reservas', ruta: RUTAS.reservas },
  { etiqueta: 'Nueva reserva', ruta: RUTAS.nuevaReservaRecepcion },
  { etiqueta: 'Calendario de ocupación', ruta: RUTAS.calendarioOcupacion },
] as const;

const OPCIONES_ADMINISTRACION = [
  { etiqueta: 'Alojamiento', ruta: RUTAS.alojamiento },
  { etiqueta: 'Apartamentos', ruta: RUTAS.apartamentos },
  { etiqueta: 'Bloqueos', ruta: RUTAS.bloqueos },
  { etiqueta: 'Temporadas y tarifas', ruta: RUTAS.temporadasTarifas },
  { etiqueta: 'Política de cancelación', ruta: RUTAS.politicaCancelacion },
  { etiqueta: 'Canales', ruta: RUTAS.canales },
  { etiqueta: 'Conflictos', ruta: RUTAS.conflictosCanal },
  { etiqueta: 'Bitácora del canal', ruta: RUTAS.bitacoraCanal },
  { etiqueta: 'Usuarios', ruta: RUTAS.usuarios },
  { etiqueta: 'Notificaciones', ruta: RUTAS.notificaciones },
  { etiqueta: 'Reportes', ruta: RUTAS.reportes },
] as const;

const OPCIONES_USUARIO = [{ etiqueta: 'Mi perfil', ruta: RUTAS.perfil }] as const;

const ETIQUETA_ROL: Record<RolUsuario, string> = {
  HUESPED: 'Huésped',
  RECEPCIONISTA: 'Recepción',
  ADMINISTRADOR: 'Administrador',
  PERSONAL_SERVICIO: 'Servicio',
};

function claseEnlace({ isActive }: { isActive: boolean }) {
  return `nav-link ${isActive ? 'active fw-semibold' : ''}`;
}

/**
 * Barra de navegación responsiva de Bootstrap con la identidad de Hospeda. Cada opción aparece solo para el rol que
 * puede usarla (las mismas reglas de rutas/acceso.ts); sin sesión solo se ve el inicio y los botones de ingreso.
 * Ocultar una opción no protege nada: la seguridad real es del backend.
 */
export function Encabezado({ nombreAlojamiento }: EncabezadoProps) {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const navigate = useNavigate();
  const { sesion, tieneRol, cerrarSesion } = useAuth();
  const foto = useFotoPerfil();

  // La foto de perfil no viaja en el token: se pide con el perfil al iniciar sesión. Si falla, se ven las iniciales.
  const token = sesion?.token ?? null;
  useEffect(() => {
    if (!token) {
      establecerFotoDePerfil(null);
      return;
    }
    const controlador = new AbortController();
    obtenerMiPerfil(controlador.signal)
      .then((perfil) => establecerFotoDePerfil(perfil.fotoUrl))
      .catch(() => establecerFotoDePerfil(null));
    return () => controlador.abort();
  }, [token]);

  function salir() {
    cerrarSesion();
    setMenuAbierto(false);
    navigate(RUTAS.login);
  }

  return (
    <header>
      <nav className="navbar navbar-expand-lg bg-body-tertiary border-bottom">
        <div className="container">
          <Link to={RUTAS.inicio} id="marca" className="navbar-brand d-flex align-items-center gap-2">
            <svg width="28" height="28" viewBox="0 0 64 64" aria-hidden="true">
              <rect width="64" height="64" rx="14" fill="var(--hospeda-verde)" />
              <path d="M12 32 32 14l20 18v18a2 2 0 0 1-2 2H14a2 2 0 0 1-2-2z" fill="#fff" />
              <path d="M26 52V38h12v14z" fill="var(--hospeda-verde)" />
            </svg>
            {nombreAlojamiento}
          </Link>

          <button
            className="navbar-toggler"
            type="button"
            aria-controls="menuPrincipal"
            aria-expanded={menuAbierto}
            aria-label="Abrir menú"
            onClick={() => setMenuAbierto(!menuAbierto)}
          >
            <span className="navbar-toggler-icon"></span>
          </button>

          <div id="menuPrincipal" className={`collapse navbar-collapse ${menuAbierto ? 'show' : ''}`}>
            <ul className="navbar-nav me-auto" onClick={() => setMenuAbierto(false)}>
              <li className="nav-item">
                <NavLink to={RUTAS.inicio} end className={claseEnlace}>Inicio</NavLink>
              </li>
              {tieneRol('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR') && (
                <li className="nav-item">
                  <NavLink to={RUTAS.buscar} className={claseEnlace}>Buscar</NavLink>
                </li>
              )}
              {tieneRol('HUESPED') && (
                <li className="nav-item">
                  <NavLink to={RUTAS.misReservas} className={claseEnlace}>Mis reservas</NavLink>
                </li>
              )}
              {tieneRol('RECEPCIONISTA', 'ADMINISTRADOR') && (
                <MenuDesplegable titulo="Recepción" opciones={OPCIONES_RECEPCION} />
              )}
              {tieneRol('PERSONAL_SERVICIO', 'ADMINISTRADOR') && (
                <li className="nav-item">
                  <NavLink to={RUTAS.preparacion} className={claseEnlace}>Preparación</NavLink>
                </li>
              )}
              {tieneRol('ADMINISTRADOR') && <MenuDesplegable titulo="Administración" opciones={OPCIONES_ADMINISTRACION} />}
            </ul>

            {sesion ? (
              <div className="d-flex align-items-center gap-2">
                {/* El menú del usuario: su perfil y el cambio de contraseña */}
                <ul className="navbar-nav" onClick={() => setMenuAbierto(false)}>
                  <MenuDesplegable titulo={
                      <span className="d-inline-flex align-items-center gap-2">
                        <Avatar nombre={sesion.nombre ?? sesion.correo} fotoUrl={foto} tamano={28} />
                        {sesion.nombre ?? sesion.correo ?? 'Mi cuenta'}
                      </span>
                    } opciones={OPCIONES_USUARIO} alineadoALaDerecha />
                </ul>
                <span className="navbar-text small">
                  {sesion.roles.map((rol) => (
                    <span key={rol} className="badge text-bg-light border">{ETIQUETA_ROL[rol]}</span>
                  ))}
                </span>
                <button type="button" className="btn btn-outline-danger btn-sm" onClick={salir}>
                  Cerrar sesión
                </button>
              </div>
            ) : (
              <div className="d-flex gap-2" onClick={() => setMenuAbierto(false)}>
                <Link to={RUTAS.login} className="btn btn-primary btn-sm">Ingresar</Link>
                <Link to={RUTAS.registro} className="btn btn-outline-secondary btn-sm">Registrarse</Link>
              </div>
            )}
          </div>
        </div>
      </nav>
    </header>
  );
}
