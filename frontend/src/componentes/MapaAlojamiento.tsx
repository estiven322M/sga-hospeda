import 'leaflet/dist/leaflet.css';
import { icon } from 'leaflet';
import marcadorRetina from 'leaflet/dist/images/marker-icon-2x.png';
import marcador from 'leaflet/dist/images/marker-icon.png';
import sombra from 'leaflet/dist/images/marker-shadow.png';
import { useEffect, useState } from 'react';
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet';

// Con un empaquetador, Leaflet no encuentra solo las imágenes de su marcador: se le indican (Vite las convierte en URL).
const iconoDelAlojamiento = icon({
  iconUrl: marcador,
  iconRetinaUrl: marcadorRetina,
  shadowUrl: sombra,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

/** Cuántos mosaicos seguidos pueden fallar, sin que cargue ninguno, antes de dar el mapa por perdido. */
const ERRORES_TOLERADOS = 3;

interface MapaAlojamientoProps {
  latitud: number;
  longitud: number;
  nombre: string;
  /** Se llama cuando el mapa no logra descargar mosaicos (sin internet): quien lo usa muestra la dirección en su lugar. */
  alFallar: () => void;
}

/**
 * Mapa de OpenStreetMap con Leaflet (react-leaflet): no necesita cuenta ni clave. Es un módulo aparte que se descarga bajo
 * demanda (lazy), porque Leaflet pesa y no todas las pantallas lo usan. Los mosaicos los pide el navegador a tile.openstreetmap.org:
 * si ninguno carga, avisa con alFallar y la página muestra la dirección con las coordenadas.
 */
export default function MapaAlojamiento({ latitud, longitud, nombre, alFallar }: MapaAlojamientoProps) {
  const [cargados, setCargados] = useState(0);
  const [errores, setErrores] = useState(0);

  useEffect(() => {
    if (cargados === 0 && errores >= ERRORES_TOLERADOS) alFallar();
  }, [cargados, errores, alFallar]);

  return (
    <MapContainer center={[latitud, longitud]} zoom={16} scrollWheelZoom={false} style={{ height: 300, width: '100%' }} className="rounded border">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">Colaboradores de OpenStreetMap</a>'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        eventHandlers={{
          tileload: () => setCargados((n) => n + 1),
          tileerror: () => setErrores((n) => n + 1),
        }}
      />
      <Marker position={[latitud, longitud]} icon={iconoDelAlojamiento}>
        <Popup>{nombre}</Popup>
      </Marker>
    </MapContainer>
  );
}
