/** El enlace a OpenStreetMap centrado en unas coordenadas: sirve aunque el mapa embebido no cargue. */
export function enlaceOpenStreetMap(latitud: number, longitud: number): string {
  return `https://www.openstreetmap.org/?mlat=${latitud}&mlon=${longitud}#map=17/${latitud}/${longitud}`;
}
