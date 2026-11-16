/**
 * Una imagen de la galería de un apartamento (7.3): espejo de ImagenResponse del openapi.yaml. `id` sirve para elegirla como
 * principal o retirarla; la galería trae solo las imágenes sin retirar, en el orden en que se agregaron, y exactamente una
 * es la principal.
 */
export interface ImagenResponse {
  id: string;
  /** URL pública de la imagen (almacén local o Cloudinary). */
  url: string;
  principal: boolean;
}

/** Máximo de imágenes por apartamento (7.3). El backend lo vuelve a revisar: esto solo avisa antes de subir. */
export const MAXIMO_IMAGENES = 10;
