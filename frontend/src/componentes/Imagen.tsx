import { useState } from 'react';

interface ImagenProps {
  /** URL de la imagen; nula si no hay ninguna (por ejemplo, un apartamento sin imágenes). */
  src: string | null | undefined;
  alt: string;
  className?: string;
  /** Texto del marcador que se ve cuando no hay imagen o no carga. */
  textoSinImagen?: string;
}

/**
 * Una imagen que NUNCA rompe la página: si no hay URL o el servidor de imágenes no responde (sin internet, almacén caído),
 * se ve un marcador gris en su lugar, del mismo tamaño. Un fallo del almacén de imágenes no puede impedir usar la pantalla (I-05).
 * El estado de "falló" se guarda junto a la URL que falló: si la URL cambia, se vuelve a intentar.
 */
export function Imagen({ src, alt, className = '', textoSinImagen = 'Sin imagen' }: ImagenProps) {
  const [fallida, setFallida] = useState<string | null>(null);
  const mostrar = src && fallida !== src;

  if (!mostrar) {
    return (
      <div
        className={`d-flex align-items-center justify-content-center bg-body-secondary text-body-secondary small ${className}`}
        role="img"
        aria-label={`${alt} (${textoSinImagen.toLowerCase()})`}
      >
        {textoSinImagen}
      </div>
    );
  }

  return <img src={src} alt={alt} className={className} loading="lazy" onError={() => setFallida(src)} />;
}
