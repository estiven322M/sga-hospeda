import type { ReactNode } from 'react';

interface PanelAccesoProps {
  titulo: string;
  subtitulo?: string;
  children: ReactNode;
}

/**
 * Diseño de las pantallas de acceso (ingresar, crear cuenta, recuperar contraseña): a la izquierda una foto del Eje
 * Cafetero con el logo del sistema (solo en pantallas anchas) y a la derecha el formulario. Es solo presentación: el formulario y su lógica los pone cada
 * página como `children`.
 */
export function PanelAcceso({ titulo, subtitulo, children }: PanelAccesoProps) {
  return (
    <section className="panel-acceso row g-0 mx-auto">
      <aside className="panel-acceso__marca col-lg-5 d-none d-lg-flex flex-column justify-content-between">
        <div className="panel-acceso__tarjeta-logo">
          <img src="/logo-sga.png" alt="Logo de SGA, Sistema de Gestión de Alojamiento" className="panel-acceso__logo" />
        </div>
        <p className="panel-acceso__lema mb-0">Apartamentos autónomos en el corazón del Eje Cafetero.</p>
      </aside>

      <div className="panel-acceso__formulario col-12 col-lg-7">
        <h1 className="h3 mb-1">{titulo}</h1>
        {subtitulo && <p className="text-body-secondary mb-4">{subtitulo}</p>}
        {children}
      </div>
    </section>
  );
}
