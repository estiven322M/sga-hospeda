import { useState } from 'react';
import { NavLink } from 'react-router';

interface OpcionMenu {
  etiqueta: string;
  ruta: string;
}

interface MenuDesplegableProps {
  titulo: string;
  opciones: readonly OpcionMenu[];
  /** El menú se abre hacia la izquierda (el del usuario, pegado al borde derecho de la barra). */
  alineadoALaDerecha?: boolean;
}

/**
 * Menú desplegable de la barra de navegación, controlado con estado de React (sin el JavaScript de Bootstrap,
 * 3.4): solo usa sus clases. Se cierra al elegir una opción.
 */
export function MenuDesplegable({ titulo, opciones, alineadoALaDerecha = false }: MenuDesplegableProps) {
  const [abierto, setAbierto] = useState(false);

  return (
    <li className="nav-item dropdown">
      <button
        type="button"
        className="nav-link dropdown-toggle btn btn-link"
        aria-expanded={abierto}
        onClick={() => setAbierto(!abierto)}
      >
        {titulo}
      </button>
      <ul className={`dropdown-menu ${alineadoALaDerecha ? 'dropdown-menu-end' : ''} ${abierto ? 'show' : ''}`}>
        {opciones.map((opcion) => (
          <li key={opcion.ruta}>
            <NavLink to={opcion.ruta} className="dropdown-item" onClick={() => setAbierto(false)}>
              {opcion.etiqueta}
            </NavLink>
          </li>
        ))}
      </ul>
    </li>
  );
}
