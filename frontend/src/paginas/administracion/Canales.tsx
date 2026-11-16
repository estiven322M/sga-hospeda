import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Campo } from '../../componentes/Campo';
import { CampoSelect } from '../../componentes/CampoSelect';
import { Cargando } from '../../componentes/Cargando';
import { Paginacion } from '../../componentes/Paginacion';
import { esquemaCanal, type FormularioCanal } from '../../esquemas/canales';
import { useConsulta } from '../../hooks/useConsulta';
import { TIPOS_CANAL, type CanalVentaCreadoResponse, type CanalVentaResponse, type TipoCanalVenta } from '../../modelos/canal';
import { activarCanal, crearCanal, desactivarCanal, listarCanales } from '../../servicios/canalServicio';
import { erroresDeCampo, mensajeDeError } from '../../utilidades/errores';
import { confirmarAccion, notificarError, notificarExito } from '../../utilidades/notificaciones';

const ETIQUETA_TIPO: Record<TipoCanalVenta, string> = {
  PLATAFORMA_EN_LINEA: 'Plataforma en línea',
  AGENCIA_DE_VIAJES: 'Agencia de viajes',
  OTRO: 'Otro',
};

const CAMPOS = ['codigo', 'nombre', 'tipo'] as const;

/** La credencial recién creada: se muestra UNA sola vez; después el sistema solo conoce su hash. */
function CredencialNueva({ canal, alCerrar }: { canal: CanalVentaCreadoResponse; alCerrar: () => void }) {
  const [copiada, setCopiada] = useState(false);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(canal.credencial);
      setCopiada(true);
    } catch {
      notificarError('No se pudo copiar automáticamente: selecciónela y cópiela a mano');
    }
  }

  return (
    <div className="alert alert-warning" role="alert" aria-label="Credencial del canal">
      <h2 className="h6">Credencial del canal {canal.codigo}</h2>
      <p className="mb-2">
        <strong>Esta es la única vez que se muestra.</strong> El sistema solo guarda su huella: si se pierde hay que crear otro canal.
        Entréguesela al canal; la envía en el encabezado <code>X-Canal-Credencial</code>.
      </p>
      <div className="input-group mb-2">
        <input className="form-control font-monospace" readOnly value={canal.credencial} aria-label="Credencial" onFocus={(e) => e.currentTarget.select()} />
        <button type="button" className="btn btn-outline-dark" onClick={copiar}>
          {copiada ? 'Copiada' : 'Copiar'}
        </button>
      </div>
      <button type="button" className="btn btn-dark btn-sm" onClick={alCerrar}>
        Ya la copié
      </button>
    </div>
  );
}

function FormularioCanalNuevo({ alCrear, alCancelar }: { alCrear: (canal: CanalVentaCreadoResponse) => void; alCancelar: () => void }) {
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioCanal>({
    resolver: zodResolver(esquemaCanal),
    mode: 'onChange',
    defaultValues: { codigo: '', nombre: '', tipo: 'AGENCIA_DE_VIAJES' },
  });

  async function enviar(datos: FormularioCanal) {
    setErrorServidor(null);
    try {
      alCrear(await crearCanal(datos));
    } catch (error) {
      setErrorServidor(mensajeDeError(error));
      for (const [campo, mensaje] of Object.entries(erroresDeCampo(error))) {
        if ((CAMPOS as readonly string[]).includes(campo)) setError(campo as (typeof CAMPOS)[number], { message: mensaje });
      }
    }
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate aria-label="Nuevo canal" className="card card-body mb-4">
      <h2 className="h5">Nuevo canal de venta</h2>
      {errorServidor && <div className="alert alert-danger" role="alert">{errorServidor}</div>}
      <div className="row">
        <div className="col-12 col-md-4">
          <Campo id="canal-codigo" etiqueta="Código" autoComplete="off" error={errors.codigo?.message} {...register('codigo')} />
        </div>
        <div className="col-12 col-md-5">
          <Campo id="canal-nombre" etiqueta="Nombre" autoComplete="off" error={errors.nombre?.message} {...register('nombre')} />
        </div>
        <div className="col-12 col-md-3">
          <CampoSelect id="canal-tipo" etiqueta="Tipo" error={errors.tipo?.message} {...register('tipo')}>
            {TIPOS_CANAL.map((tipo) => (
              <option key={tipo} value={tipo}>{ETIQUETA_TIPO[tipo]}</option>
            ))}
          </CampoSelect>
        </div>
      </div>
      <div className="d-flex gap-2">
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
          {isSubmitting ? 'Creando…' : 'Crear canal'}
        </button>
        <button type="button" className="btn btn-outline-secondary" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}

/**
 * Canales de venta externos (7.8): se registra cada canal con su código único, se muestra su credencial una sola vez y se
 * desactiva (baja lógica) o activa. Un canal desactivado recibe 401 con su credencial hasta que se vuelva a activar.
 */
export default function Canales() {
  const [pagina, setPagina] = useState(0);
  const [creando, setCreando] = useState(false);
  const [recienCreado, setRecienCreado] = useState<CanalVentaCreadoResponse | null>(null);
  const [ocupado, setOcupado] = useState<string | null>(null);
  const { resultado, cargando, recargar } = useConsulta(String(pagina), (signal) => listarCanales(pagina, signal));

  async function cambiarActividad(canal: CanalVentaResponse) {
    if (canal.activo) {
      const confirmado = await confirmarAccion(
        `Desactivar el canal ${canal.codigo}`,
        'Su credencial dejará de servir (recibirá 401) y quedará registrado cada intento. Es una baja lógica: el canal, su historial y sus reservas se conservan y se puede activar de nuevo.',
      );
      if (!confirmado) return;
    }
    setOcupado(canal.codigo);
    try {
      await (canal.activo ? desactivarCanal(canal.codigo) : activarCanal(canal.codigo));
      notificarExito(canal.activo ? `Canal ${canal.codigo} desactivado` : `Canal ${canal.codigo} activado`);
      recargar();
    } catch (error) {
      notificarError(mensajeDeError(error));
    } finally {
      setOcupado(null);
    }
  }

  return (
    <section>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
        <h1 className="h3 mb-0">Canales de venta</h1>
        {!creando && (
          <button type="button" className="btn btn-primary" onClick={() => setCreando(true)}>
            Nuevo canal
          </button>
        )}
      </div>
      <p className="text-body-secondary">
        Plataformas y agencias que venden los apartamentos por la API del canal. Cada una entra con su credencial; los mensajes que
        envía quedan en la bitácora y los conflictos con reservas vigentes, en Conflictos.
      </p>

      {recienCreado && <CredencialNueva canal={recienCreado} alCerrar={() => setRecienCreado(null)} />}
      {creando && (
        <FormularioCanalNuevo
          alCrear={(canal) => {
            setCreando(false);
            setRecienCreado(canal);
            setPagina(0);
            recargar();
            notificarExito(`Canal ${canal.codigo} creado`);
          }}
          alCancelar={() => setCreando(false)}
        />
      )}

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">Aún no hay canales registrados.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'canal' : 'canales'}
              </p>
              <div className="table-responsive">
                <table className="table align-middle">
                  <thead>
                    <tr>
                      <th scope="col">Código</th>
                      <th scope="col">Nombre</th>
                      <th scope="col">Tipo</th>
                      <th scope="col">Estado</th>
                      <th scope="col"><span className="visually-hidden">Acciones</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {resultado.datos.contenido.map((canal) => (
                      <tr key={canal.codigo}>
                        <th scope="row">{canal.codigo}</th>
                        <td>{canal.nombre}</td>
                        <td>{ETIQUETA_TIPO[canal.tipo]}</td>
                        <td>
                          <span className={`badge ${canal.activo ? 'text-bg-success' : 'text-bg-secondary'}`}>
                            {canal.activo ? 'Activo' : 'Desactivado'}
                          </span>
                        </td>
                        <td className="text-end">
                          <button
                            type="button"
                            className={`btn btn-sm ${canal.activo ? 'btn-outline-danger' : 'btn-outline-primary'}`}
                            disabled={ocupado === canal.codigo}
                            onClick={() => cambiarActividad(canal)}
                          >
                            {canal.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
            </>
          ))}
      </div>
    </section>
  );
}
