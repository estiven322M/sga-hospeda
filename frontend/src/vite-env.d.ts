// Tipos de las variables de entorno propias: así import.meta.env.VITE_API_URL es un string conocido
// por TypeScript, con autocompletado, y un nombre mal escrito falla al compilar.
interface ImportMetaEnv {
  /** URL base de la API, con /api (p. ej. http://localhost:8080/api). */
  readonly VITE_API_URL: string;
  /** Nombre del alojamiento que se muestra en el encabezado. */
  readonly VITE_NOMBRE_ALOJAMIENTO: string;
  /**
   * Edad desde la que alguien cuenta como adulto responsable. SOLO se usa para AVISAR en el formulario de reserva:
   * la regla real (RP-03) y su umbral viven en el backend, que es quien acepta o rechaza la reserva.
   */
  readonly VITE_EDAD_ADULTO_RESPONSABLE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
