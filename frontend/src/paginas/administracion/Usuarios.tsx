import { useState } from 'react';
import { Cargando } from '../../componentes/Cargando';
import { CampoSelect } from '../../componentes/CampoSelect';
import { FormularioUsuarioNuevo } from '../../componentes/FormularioUsuarioInterno';
import { Paginacion } from '../../componentes/Paginacion';
import { useAuth } from '../../hooks/useAuth';
import { useConsulta } from '../../hooks/useConsulta';
import { ROLES, type RolUsuario } from '../../modelos/auth';
import type { UsuarioResponse } from '../../modelos/usuario';
import { cambiarRolUsuario, desactivarUsuario, listarUsuarios } from '../../servicios/usuarioServicio';
import { mensajeDeError } from '../../utilidades/errores';
import { formatearFecha } from '../../utilidades/formato';
import { confirmarAccion, notificarError, notificarExito } from '../../utilidades/notificaciones';

const ETIQUETA_ROL: Record<RolUsuario, string> = {
  HUESPED: 'Huésped',
  RECEPCIONISTA: 'Recepcionista',
  ADMINISTRADOR: 'Administrador',
  PERSONAL_SERVICIO: 'Personal de servicio',
};

interface TarjetaUsuarioProps {
  usuario: UsuarioResponse;
  esElPropio: boolean;
  alCambiar: () => void;
}

/** Un usuario con sus dos acciones: cambiar el rol y desactivar. Las reglas (no uno mismo, no un titular con reservas activas) las decide el backend. */
function TarjetaUsuario({ usuario, esElPropio, alCambiar }: TarjetaUsuarioProps) {
  const [rolElegido, setRolElegido] = useState<RolUsuario>(usuario.rol);
  const [ocupado, setOcupado] = useState(false);

  async function cambiarRol() {
    const confirmado = await confirmarAccion(
      'Cambiar el rol',
      `${usuario.nombre} pasará de ${ETIQUETA_ROL[usuario.rol]} a ${ETIQUETA_ROL[rolElegido]}. El nuevo rol rige desde su próximo inicio de sesión.`,
    );
    if (!confirmado) return;
    setOcupado(true);
    try {
      await cambiarRolUsuario(usuario.id, rolElegido);
      notificarExito('Rol actualizado');
      alCambiar();
    } catch (error) {
      notificarError(mensajeDeError(error));
    } finally {
      setOcupado(false);
    }
  }

  async function desactivar() {
    const confirmado = await confirmarAccion(
      'Desactivar el usuario',
      `${usuario.nombre} ya no podrá iniciar sesión. Es una eliminación lógica: sus datos y sus reservas se conservan. No se puede desactivar a quien es titular de reservas pendientes, confirmadas o en curso.`,
    );
    if (!confirmado) return;
    setOcupado(true);
    try {
      await desactivarUsuario(usuario.id);
      notificarExito('Usuario desactivado');
      alCambiar();
    } catch (error) {
      // 409: el mensaje de la regla (titular con reservas activas, o uno mismo)
      notificarError(mensajeDeError(error));
    } finally {
      setOcupado(false);
    }
  }

  return (
    <article className="card mb-3" aria-label={`Usuario ${usuario.correo}`}>
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h2 className="h6 mb-0">{usuario.nombre}</h2>
            <span className="text-body-secondary small">{usuario.correo}</span>
          </div>
          <div className="d-flex gap-1">
            <span className="badge text-bg-light border">{ETIQUETA_ROL[usuario.rol]}</span>
            {usuario.activo ? (
              <span className="badge text-bg-success">Activo</span>
            ) : (
              <span className="badge text-bg-secondary">Desactivado</span>
            )}
            {esElPropio && <span className="badge text-bg-info">Usted</span>}
          </div>
        </div>
        <ul className="list-unstyled small text-body-secondary mt-2 mb-2">
          <li>Documento: {usuario.documento}</li>
          <li>Teléfono: {usuario.telefono}</li>
          <li>Nacimiento: {formatearFecha(usuario.fechaNacimiento)}</li>
        </ul>

        {usuario.activo && !esElPropio && (
          <div className="d-flex flex-wrap align-items-end gap-2">
            <div style={{ minWidth: 220 }}>
              <CampoSelect
                id={`rol-${usuario.id}`}
                etiqueta="Rol"
                className="form-select-sm"
                value={rolElegido}
                onChange={(evento) => setRolElegido(evento.target.value as RolUsuario)}
              >
                {ROLES.map((rol) => (
                  <option key={rol} value={rol}>{ETIQUETA_ROL[rol]}</option>
                ))}
              </CampoSelect>
            </div>
            <button type="button" className="btn btn-outline-primary btn-sm mb-3" disabled={ocupado || rolElegido === usuario.rol} onClick={cambiarRol}>
              Cambiar rol
            </button>
            <button type="button" className="btn btn-outline-danger btn-sm mb-3" disabled={ocupado} onClick={desactivar}>
              Desactivar
            </button>
          </div>
        )}
        {esElPropio && <p className="small text-body-secondary mb-0">No puede cambiar su propio rol ni desactivar su cuenta.</p>}
      </div>
    </article>
  );
}

/**
 * Administración de usuarios (7.2): listado de 10 en 10 con filtro por rol, creación de usuarios internos, cambio de rol y
 * desactivación lógica (nunca a un titular con reservas activas). El servidor decide cada regla y la pantalla muestra su mensaje.
 */
export default function Usuarios() {
  const { sesion } = useAuth();
  const [rol, setRol] = useState<RolUsuario | ''>('');
  const [pagina, setPagina] = useState(0);
  const [creando, setCreando] = useState(false);
  const { resultado, cargando, recargar } = useConsulta(`${rol}|${pagina}`, (signal) => listarUsuarios(rol, pagina, signal));

  return (
    <section>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h1 className="h3 mb-0">Usuarios</h1>
        {!creando && (
          <button type="button" className="btn btn-primary" onClick={() => setCreando(true)}>Nuevo usuario</button>
        )}
      </div>

      {creando && (
        <FormularioUsuarioNuevo
          alCancelar={() => setCreando(false)}
          alCrear={() => {
            setCreando(false);
            setPagina(0);
            recargar();
          }}
        />
      )}

      <div className="row">
        <div className="col-12 col-md-4">
          <CampoSelect
            id="filtro-rol"
            etiqueta="Filtrar por rol"
            value={rol}
            onChange={(evento) => {
              setRol(evento.target.value as RolUsuario | '');
              setPagina(0);
            }}
          >
            <option value="">Todos los roles</option>
            {ROLES.map((opcion) => (
              <option key={opcion} value={opcion}>{ETIQUETA_ROL[opcion]}</option>
            ))}
          </CampoSelect>
        </div>
      </div>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
        {resultado?.tipo === 'datos' &&
          (resultado.datos.contenido.length === 0 ? (
            <div className="alert alert-secondary">No hay usuarios con ese rol.</div>
          ) : (
            <>
              <p className="text-body-secondary small">
                {resultado.datos.totalElementos} {resultado.datos.totalElementos === 1 ? 'usuario' : 'usuarios'}
              </p>
              {resultado.datos.contenido.map((usuario) => (
                <TarjetaUsuario
                  key={`${usuario.id}|${usuario.rol}|${usuario.activo}`}
                  usuario={usuario}
                  esElPropio={usuario.correo === sesion?.correo}
                  alCambiar={recargar}
                />
              ))}
              <Paginacion paginaActual={resultado.datos.pagina} totalPaginas={resultado.datos.totalPaginas} alCambiar={setPagina} />
            </>
          ))}
      </div>
    </section>
  );
}
