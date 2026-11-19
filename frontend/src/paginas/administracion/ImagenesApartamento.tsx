import { useEffect, useState, type ChangeEvent } from 'react';
import { Link, useParams } from 'react-router';
import { Cargando } from '../../componentes/Cargando';
import { Imagen } from '../../componentes/Imagen';
import { useConsulta } from '../../hooks/useConsulta';
import type { ApartamentoResponse } from '../../modelos/apartamento';
import { MAXIMO_IMAGENES } from '../../modelos/imagen';
import { RUTAS } from '../../rutas/rutas';
import {
  establecerImagenPrincipal,
  obtenerApartamento,
  retirarImagen,
  subirImagenes,
} from '../../servicios/apartamentoServicio';
import { ACEPTAR_IMAGENES, MAXIMO_APARTAMENTO_MB, validarArchivosDeImagen } from '../../utilidades/archivosImagen';
import { mensajeDeError } from '../../utilidades/errores';
import { confirmarAccion, notificarError, notificarExito } from '../../utilidades/notificaciones';

interface VistaPrevia {
  archivo: File;
  url: string;
}

/**
 * Imágenes de un apartamento (7.3): de 1 a 10, con una principal. El administrador sube varias a la vez (FormData, con vista
 * previa y barra de progreso), elige la principal y retira las que sobran (retiro lógico). Se revisa el tipo, el peso y el cupo
 * antes de enviar, pero quien decide es el backend: reconoce el formato por el contenido, aplica el tamaño máximo y el límite de
 * 10 (409), no deja sin imágenes a un apartamento activo (409) y responde 503 si el almacén externo no responde.
 */
export default function ImagenesApartamento() {
  const { identificacion = '' } = useParams();
  const { resultado, cargando, recargar } = useConsulta(`imagenes|${identificacion}`, (signal) => obtenerApartamento(identificacion, signal));
  // Lo que devolvió la última acción: se muestra sin volver a consultar
  const [actualizado, setActualizado] = useState<ApartamentoResponse | null>(null);
  const apartamento = actualizado?.identificacion === identificacion ? actualizado : resultado?.tipo === 'datos' ? resultado.datos : null;

  const [elegidos, setElegidos] = useState<VistaPrevia[]>([]);
  const [problema, setProblema] = useState<string | null>(null);
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [progreso, setProgreso] = useState<number | null>(null);
  const [ocupado, setOcupado] = useState(false);

  // Las vistas previas son URL de objeto: se liberan al cambiar la selección o al salir
  useEffect(() => () => elegidos.forEach((vista) => URL.revokeObjectURL(vista.url)), [elegidos]);

  const imagenes = apartamento?.imagenes ?? [];
  const cupo = MAXIMO_IMAGENES - imagenes.length;

  function alElegir(evento: ChangeEvent<HTMLInputElement>) {
    const archivos = [...(evento.target.files ?? [])];
    setErrorServidor(null);
    const error = validarArchivosDeImagen(archivos, MAXIMO_APARTAMENTO_MB, cupo);
    setProblema(archivos.length === 0 ? null : error);
    if (error || archivos.length === 0) {
      setElegidos([]);
      if (error) evento.target.value = '';
      return;
    }
    setElegidos(archivos.map((archivo) => ({ archivo, url: URL.createObjectURL(archivo) })));
  }

  async function subir() {
    setOcupado(true);
    setErrorServidor(null);
    setProgreso(0);
    try {
      const respuesta = await subirImagenes(identificacion, elegidos.map((vista) => vista.archivo), setProgreso);
      setActualizado(respuesta);
      setElegidos([]);
      notificarExito(elegidos.length === 1 ? 'Imagen subida' : `${elegidos.length} imágenes subidas`);
    } catch (error) {
      // 409: la regla (formato, peso, máximo 10). 503: el almacén de imágenes no responde y no se registró ninguna del lote
      setErrorServidor(mensajeDeError(error));
    } finally {
      setOcupado(false);
      setProgreso(null);
    }
  }

  async function accion(ejecutar: () => Promise<ApartamentoResponse>, exito: string) {
    setOcupado(true);
    setErrorServidor(null);
    try {
      setActualizado(await ejecutar());
      notificarExito(exito);
    } catch (error) {
      notificarError(mensajeDeError(error));
    } finally {
      setOcupado(false);
    }
  }

  async function retirar(idImagen: string) {
    const confirmado = await confirmarAccion(
      'Retirar la imagen',
      'La imagen sale de la galería. Es un retiro lógico: el registro y el archivo se conservan. Un apartamento activo debe conservar al menos una imagen.',
    );
    if (confirmado) await accion(() => retirarImagen(identificacion, idImagen), 'Imagen retirada');
  }

  if (cargando && !apartamento) return <Cargando />;
  if (resultado?.tipo === 'error' && !apartamento) {
    return (
      <section>
        <h1 className="h3">Imágenes del apartamento</h1>
        <div className="alert alert-danger" role="alert">{resultado.estadoHttp === 404 ? `No existe el apartamento ${identificacion}.` : resultado.mensaje}</div>
        <Link to={RUTAS.apartamentos} className="btn btn-outline-primary">Volver a los apartamentos</Link>
      </section>
    );
  }
  if (!apartamento) return null;

  return (
    <section>
      <h1 className="h3 mb-1">Imágenes de {apartamento.identificacion}</h1>
      <p className="text-body-secondary">
        {apartamento.nombre} ·{' '}
        <span className={`badge ${apartamento.activo ? 'text-bg-success' : 'text-bg-secondary'}`}>{apartamento.activo ? 'Activo' : 'Inactivo'}</span> ·{' '}
        {imagenes.length} de {MAXIMO_IMAGENES} imágenes
      </p>
      {!apartamento.activo && imagenes.length === 0 && (
        <div className="alert alert-info">Para activar el apartamento hace falta al menos una imagen (además de las tarifas de todas las temporadas).</div>
      )}

      <div className="card mb-4">
        <div className="card-body">
          <h2 className="h5 card-title">Subir imágenes</h2>
          <p className="text-body-secondary small">
            Hasta {MAXIMO_IMAGENES} imágenes por apartamento, JPG, PNG o WEBP de hasta {MAXIMO_APARTAMENTO_MB} MB cada una. La primera que se sube a un
            apartamento sin imágenes queda como principal. Puede elegir varias a la vez; aún caben {cupo}.
          </p>
          {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
          <input
            id="imagenes-subir"
            type="file"
            multiple
            accept={ACEPTAR_IMAGENES}
            className={`form-control mb-2 ${problema ? 'is-invalid' : ''}`}
            aria-label="Elegir imágenes"
            aria-invalid={problema ? true : undefined}
            onChange={alElegir}
            disabled={ocupado || cupo <= 0}
          />
          <div className="invalid-feedback">{problema}</div>
          {elegidos.length > 0 && (
            <>
              <div className="d-flex flex-wrap gap-2 my-3" aria-label="Vista previa de las imágenes elegidas">
                {elegidos.map((vista) => (
                  <img key={vista.url} src={vista.url} alt={vista.archivo.name} title={vista.archivo.name} style={{ width: 96, height: 72, objectFit: 'cover' }} className="rounded border" />
                ))}
              </div>
              {progreso !== null && (
                <div className="progress mb-3" role="progressbar" aria-label="Progreso de la subida" aria-valuenow={Math.round(progreso * 100)} aria-valuemin={0} aria-valuemax={100}>
                  <div className="progress-bar" style={{ width: `${Math.round(progreso * 100)}%` }}>{Math.round(progreso * 100)} %</div>
                </div>
              )}
              <button type="button" className="btn btn-primary" onClick={() => void subir()} disabled={ocupado}>
                {ocupado ? 'Subiendo…' : `Subir ${elegidos.length} ${elegidos.length === 1 ? 'imagen' : 'imágenes'}`}
              </button>
            </>
          )}
        </div>
      </div>

      <h2 className="h5">Galería</h2>
      {imagenes.length === 0 ? (
        <div className="alert alert-secondary">Este apartamento aún no tiene imágenes.</div>
      ) : (
        <div className="row g-3">
          {imagenes.map((imagen, posicion) => (
            <div key={imagen.id} className="col-12 col-sm-6 col-lg-4">
              <article className={`card h-100 ${imagen.principal ? 'border-success border-2' : ''}`} aria-label={`Imagen ${posicion + 1}`}>
                <Imagen src={imagen.url} alt={`${apartamento.nombre}: imagen ${posicion + 1}`} className="card-img-top object-fit-cover imagen-administracion" />
                <div className="card-body">
                  {imagen.principal && <span className="badge text-bg-success mb-2">Principal</span>}
                  <div className="d-flex flex-wrap gap-2">
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-success"
                      disabled={ocupado || imagen.principal}
                      onClick={() => void accion(() => establecerImagenPrincipal(identificacion, imagen.id), 'Imagen principal actualizada')}
                    >
                      Elegir como principal
                    </button>
                    <button type="button" className="btn btn-sm btn-outline-danger" disabled={ocupado} onClick={() => void retirar(imagen.id)}>
                      Retirar
                    </button>
                  </div>
                </div>
              </article>
            </div>
          ))}
        </div>
      )}

      <div className="mt-4 d-flex gap-2">
        <Link to={RUTAS.apartamentos} className="btn btn-outline-secondary">Volver a los apartamentos</Link>
        <button type="button" className="btn btn-outline-primary" onClick={() => { setActualizado(null); recargar(); }} disabled={ocupado}>Actualizar</button>
      </div>
    </section>
  );
}
