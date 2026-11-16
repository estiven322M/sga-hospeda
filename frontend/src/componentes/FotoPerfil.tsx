import { useEffect, useState, type ChangeEvent } from 'react';
import { establecerFotoDePerfil } from '../hooks/useFotoPerfil';
import { cambiarMiFoto } from '../servicios/usuarioServicio';
import { ACEPTAR_IMAGENES, MAXIMO_PERFIL_MB, validarArchivosDeImagen } from '../utilidades/archivosImagen';
import { mensajeDeError } from '../utilidades/errores';
import { notificarExito } from '../utilidades/notificaciones';
import { Avatar } from './Avatar';

interface FotoPerfilProps {
  nombre: string;
  fotoUrl: string | null;
  alCambiar: (url: string | null) => void;
}

/**
 * Foto de perfil (7.2): se muestra en un círculo (sin foto, las iniciales) y se cambia eligiendo UNA imagen JPG, PNG o WEBP de hasta
 * 2 MB. Antes de subirla se revisa el tipo y el tamaño y se muestra una vista previa (URL.createObjectURL, que se libera al
 * cambiar de archivo). El backend vuelve a validar: reconoce el formato por el contenido y responde 409 con la regla; si el almacén
 * de imágenes falla responde 503 y la foto anterior se conserva.
 */
export function FotoPerfil({ nombre, fotoUrl, alCambiar }: FotoPerfilProps) {
  // El archivo elegido junto con su vista previa (una URL de objeto, creada al elegir)
  const [seleccion, setSeleccion] = useState<{ archivo: File; url: string } | null>(null);
  const archivo = seleccion?.archivo ?? null;
  const vistaPrevia = seleccion?.url ?? null;
  const [problema, setProblema] = useState<string | null>(null);
  const [subiendo, setSubiendo] = useState(false);

  // Cada URL de objeto mantiene el archivo en memoria hasta que se libera: se libera al cambiar o al desmontar
  useEffect(() => {
    return () => {
      if (vistaPrevia) URL.revokeObjectURL(vistaPrevia);
    };
  }, [vistaPrevia]);

  function alElegir(evento: ChangeEvent<HTMLInputElement>) {
    const elegido = evento.target.files?.[0] ?? null;
    setProblema(null);
    if (!elegido) {
      setSeleccion(null);
      return;
    }
    const error = validarArchivosDeImagen([elegido], MAXIMO_PERFIL_MB, 1);
    if (error) {
      setProblema(error);
      setSeleccion(null);
      evento.target.value = '';
      return;
    }
    setSeleccion({ archivo: elegido, url: URL.createObjectURL(elegido) });
  }

  async function guardar() {
    if (!archivo) return;
    setSubiendo(true);
    setProblema(null);
    try {
      const perfil = await cambiarMiFoto(archivo);
      establecerFotoDePerfil(perfil.fotoUrl);
      alCambiar(perfil.fotoUrl);
      setSeleccion(null);
      notificarExito('Foto de perfil actualizada');
    } catch (error) {
      // 409: peso o formato (mensaje de la regla). 503: el almacén de imágenes no responde (la foto anterior sigue igual)
      setProblema(mensajeDeError(error));
    } finally {
      setSubiendo(false);
    }
  }

  return (
    <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
      <Avatar nombre={nombre} fotoUrl={vistaPrevia ?? fotoUrl} tamano={88} />
      <div className="flex-grow-1">
        <label htmlFor="foto-perfil" className="form-label mb-1">Foto de perfil</label>
        <input
          id="foto-perfil"
          type="file"
          accept={ACEPTAR_IMAGENES}
          className={`form-control form-control-sm ${problema ? 'is-invalid' : ''}`}
          aria-invalid={problema ? true : undefined}
          onChange={alElegir}
          disabled={subiendo}
        />
        <div className="invalid-feedback">{problema}</div>
        <div className="form-text">Una imagen JPG, PNG o WEBP de hasta {MAXIMO_PERFIL_MB} MB. Sin foto se ven sus iniciales.</div>
        {archivo && (
          <button type="button" className="btn btn-primary btn-sm mt-2" onClick={() => void guardar()} disabled={subiendo}>
            {subiendo ? 'Subiendo…' : 'Guardar foto'}
          </button>
        )}
      </div>
    </div>
  );
}
