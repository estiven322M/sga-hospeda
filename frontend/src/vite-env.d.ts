// Tipos de las variables de entorno propias: así import.meta.env.VITE_API_URL es un string conocido
// por TypeScript, con autocompletado, y un nombre mal escrito falla al compilar.
interface ImportMetaEnv {
  /** URL base de la API, con /api (p. ej. http://localhost:8080/api). */
  readonly VITE_API_URL: string;
  /** Nombre del alojamiento que se muestra en el encabezado. */
  readonly VITE_NOMBRE_ALOJAMIENTO: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
