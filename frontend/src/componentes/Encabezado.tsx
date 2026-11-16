import { useState } from 'react';
import { Link, NavLink } from 'react-router';
import { RUTAS } from '../rutas/rutas';
import { MenuDesplegable } from './MenuDesplegable';

interface EncabezadoProps {
  nombreAlojamiento: string;
}

const OPCIONES_RECEPCION = [
  { etiqueta: 'Reservas', ruta: RUTAS.reservas },
  { etiqueta: 'Nueva reserva', ruta: RUTAS.nuevaReservaRecepcion },
] as const;

const OPCIONES_ADMINISTRACION = [
  { etiqueta: 'Apartamentos', ruta: RUTAS.apartamentos },
  { etiqueta: 'Bloqueos', ruta: RUTAS.bloqueos },
  { etiqueta: 'Temporadas y tarifas', ruta: RUTAS.temporadasTarifas },
  { etiqueta: 'Política de cancelación', ruta: RUTAS.politicaCancelacion },
  { etiqueta: 'Rechazos externos', ruta: RUTAS.rechazosExternos },
] as const;

function claseEnlace({ isActive }: { isActive: boolean }) {
  return `nav-link ${isActive ? 'active fw-semibold' : ''}`;
}

/**
 * Barra de navegación responsiva de Bootstrap con la identidad de Hospeda (marca verde y casa).
 * Por ahora muestra TODAS las secciones: cuando exista la sesión (paso J2) cada opción aparecerá solo para el
 * rol que puede usarla.
 */
export function Encabezado({ nombreAlojamiento }: EncabezadoProps) {
  const [menuAbierto, setMenuAbierto] = useState(false);

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
              <li className="nav-item">
                <NavLink to={RUTAS.buscar} className={claseEnlace}>Buscar</NavLink>
              </li>
              <li className="nav-item">
                <NavLink to={RUTAS.misReservas} className={claseEnlace}>Mis reservas</NavLink>
              </li>
              <MenuDesplegable titulo="Recepción" opciones={OPCIONES_RECEPCION} />
              <li className="nav-item">
                <NavLink to={RUTAS.preparacion} className={claseEnlace}>Preparación</NavLink>
              </li>
              <MenuDesplegable titulo="Administración" opciones={OPCIONES_ADMINISTRACION} />
            </ul>

            <div className="d-flex gap-2" onClick={() => setMenuAbierto(false)}>
              <Link to={RUTAS.login} className="btn btn-primary btn-sm">Ingresar</Link>
              <Link to={RUTAS.registro} className="btn btn-outline-secondary btn-sm">Registrarse</Link>
            </div>
          </div>
        </div>
      </nav>
    </header>
  );
}
