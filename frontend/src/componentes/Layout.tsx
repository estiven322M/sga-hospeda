import { Suspense } from 'react';
import { Outlet } from 'react-router';
import { Cargando } from './Cargando';
import { Encabezado } from './Encabezado';
import { PiePagina } from './PiePagina';

/** Diseño común de todas las páginas: encabezado, contenido y pie. */
export function Layout() {
  return (
    <>
      <Encabezado nombreAlojamiento={import.meta.env.VITE_NOMBRE_ALOJAMIENTO} />
      <main className="container py-4">
        {/* Mientras se descarga el código de una página diferida (lazy) se muestra el indicador.
            El encabezado y el pie no desaparecen: están fuera del Suspense. */}
        <Suspense fallback={<Cargando />}>
          <Outlet />
        </Suspense>
      </main>
      <PiePagina />
    </>
  );
}
