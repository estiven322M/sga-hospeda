import { lazy, Suspense, useCallback, useState } from 'react';
import { useAlojamiento } from '../hooks/useAlojamiento';
import type { AlojamientoResponse } from '../modelos/alojamiento';
import { enlaceOpenStreetMap } from '../utilidades/mapas';
import { LimiteDeError } from './LimiteDeError';

// El mapa (Leaflet) se descarga solo cuando hace falta
const MapaAlojamiento = lazy(() => import('./MapaAlojamiento'));

/** Lo que se ve cuando el mapa no carga (sin internet): la dirección, las coordenadas y un enlace. La página sigue funcionando. */
function UbicacionSinMapa({ alojamiento }: { alojamiento: AlojamientoResponse }) {
  return (
    <div className="alert alert-light border mb-0" role="status">
      <p className="fw-semibold mb-1">{alojamiento.nombre}</p>
      <p className="mb-1">{alojamiento.direccion} · {alojamiento.ciudad}</p>
      <p className="small text-body-secondary mb-2">
        Coordenadas: {alojamiento.latitud}, {alojamiento.longitud} (no se pudo cargar el mapa)
      </p>
      <a href={enlaceOpenStreetMap(alojamiento.latitud, alojamiento.longitud)} target="_blank" rel="noreferrer">
        Ver en OpenStreetMap
      </a>
    </div>
  );
}

/**
 * Ubicación del alojamiento (7.5): mapa de OpenStreetMap con su dirección debajo, a partir de la latitud y la longitud de
 * GET /alojamiento. Es opcional por diseño: si los datos del alojamiento no cargan, no se muestra nada; si el mapa no carga
 * (sin internet, módulo que no se descarga), se ve la dirección con las coordenadas y un enlace (I-05).
 */
export function MapaDelAlojamiento() {
  const { alojamiento } = useAlojamiento();
  const [sinMapa, setSinMapa] = useState(false);
  const marcarSinMapa = useCallback(() => setSinMapa(true), []);

  if (!alojamiento) return null;
  const alternativa = <UbicacionSinMapa alojamiento={alojamiento} />;
  // navigator.onLine es false sin conexión: ni se intenta pedir los mosaicos
  const sinConexion = typeof navigator !== 'undefined' && navigator.onLine === false;

  return (
    <section aria-label="Ubicación del alojamiento">
      <h2 className="h5">Ubicación</h2>
      {sinMapa || sinConexion ? (
        alternativa
      ) : (
        <LimiteDeError alternativa={alternativa}>
          <Suspense fallback={<div className="text-body-secondary small">Cargando el mapa…</div>}>
            <MapaAlojamiento latitud={alojamiento.latitud} longitud={alojamiento.longitud} nombre={alojamiento.nombre} alFallar={marcarSinMapa} />
          </Suspense>
          <p className="small text-body-secondary mt-2 mb-0">
            {alojamiento.direccion} · {alojamiento.ciudad} ·{' '}
            <a href={enlaceOpenStreetMap(alojamiento.latitud, alojamiento.longitud)} target="_blank" rel="noreferrer">Abrir en OpenStreetMap</a>
          </p>
        </LimiteDeError>
      )}
    </section>
  );
}
