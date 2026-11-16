import { useState } from 'react';
import { iniciales } from '../utilidades/archivosImagen';

interface AvatarProps {
  nombre: string | null | undefined;
  /** URL de la foto de perfil; si es nula o no carga, se ven las iniciales. */
  fotoUrl?: string | null;
  /** Lado del círculo, en píxeles. */
  tamano?: number;
}

/** La foto de perfil en un círculo; sin foto (o si no carga), las iniciales del nombre. */
export function Avatar({ nombre, fotoUrl, tamano = 32 }: AvatarProps) {
  const [fallida, setFallida] = useState<string | null>(null);
  const estilo = { width: tamano, height: tamano, fontSize: Math.round(tamano * 0.4) };

  if (fotoUrl && fallida !== fotoUrl) {
    return (
      <img
        src={fotoUrl}
        alt={`Foto de ${nombre ?? 'perfil'}`}
        className="rounded-circle object-fit-cover border"
        style={estilo}
        onError={() => setFallida(fotoUrl)}
      />
    );
  }
  return (
    <span
      className="rounded-circle d-inline-flex align-items-center justify-content-center fw-semibold text-white"
      style={{ ...estilo, backgroundColor: 'var(--hospeda-verde)' }}
      role="img"
      aria-label={`Iniciales de ${nombre ?? 'la persona'}`}
    >
      {iniciales(nombre)}
    </span>
  );
}
