import { useState, type KeyboardEvent } from 'react';
import type { ImagenResponse } from '../modelos/imagen';
import { Imagen } from './Imagen';

interface GaleriaApartamentoProps {
  imagenes: readonly ImagenResponse[];
  nombre: string;
}

/**
 * Galería de un apartamento (7.5): la imagen principal primero, una imagen grande con flechas y las miniaturas debajo. Se hace con
 * estado de React, sin el JavaScript de Bootstrap (3.4): ni el carrusel ni sus atributos data-bs-*. Se puede navegar con las
 * flechas del teclado. Sin imágenes, o si el almacén no responde, se ve un marcador y la página sigue funcionando.
 */
export function GaleriaApartamento({ imagenes, nombre }: GaleriaApartamentoProps) {
  // La principal va primero; las demás conservan el orden del servidor (sort es estable)
  const ordenadas = [...imagenes].sort((a, b) => Number(b.principal) - Number(a.principal));
  const [indice, setIndice] = useState(0);

  if (ordenadas.length === 0) {
    return <Imagen src={null} alt={`Imagen de ${nombre}`} textoSinImagen="Este apartamento aún no tiene imágenes" className="w-100 rounded galeria__principal" />;
  }

  const actual = Math.min(indice, ordenadas.length - 1);
  const imagen = ordenadas[actual];
  const total = ordenadas.length;
  const anterior = () => setIndice((actual - 1 + total) % total);
  const siguiente = () => setIndice((actual + 1) % total);

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === 'ArrowLeft') anterior();
    if (evento.key === 'ArrowRight') siguiente();
  }

  return (
    <div role="group" aria-label={`Galería de ${nombre}`} onKeyDown={alTeclear} tabIndex={0} className="mb-3">
      <div className="position-relative">
        <Imagen
          src={imagen?.url}
          alt={`${nombre}: imagen ${actual + 1} de ${total}${imagen?.principal ? ' (principal)' : ''}`}
          className="w-100 rounded object-fit-cover galeria__principal"
        />
        {total > 1 && (
          <>
            <button type="button" className="btn btn-dark btn-sm position-absolute top-50 start-0 translate-middle-y ms-2 opacity-75" onClick={anterior} aria-label="Imagen anterior">
              ‹
            </button>
            <button type="button" className="btn btn-dark btn-sm position-absolute top-50 end-0 translate-middle-y me-2 opacity-75" onClick={siguiente} aria-label="Imagen siguiente">
              ›
            </button>
          </>
        )}
        <span className="badge text-bg-dark position-absolute bottom-0 end-0 m-2 opacity-75">{actual + 1} / {total}</span>
      </div>

      {total > 1 && (
        <div className="d-flex flex-wrap gap-2 mt-2">
          {ordenadas.map((miniatura, posicion) => (
            <button
              key={miniatura.id}
              type="button"
              className={`p-0 border rounded overflow-hidden bg-transparent ${posicion === actual ? 'border-primary border-2' : ''}`}
              style={{ width: 72, height: 54 }}
              onClick={() => setIndice(posicion)}
              aria-label={`Ver la imagen ${posicion + 1}${miniatura.principal ? ' (principal)' : ''}`}
              aria-current={posicion === actual}
            >
              <Imagen src={miniatura.url} alt="" className="w-100 h-100 object-fit-cover" textoSinImagen="" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
