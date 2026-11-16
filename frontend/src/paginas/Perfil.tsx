import { useState } from 'react';
import { CambioContrasena } from '../componentes/CambioContrasena';
import { Cargando } from '../componentes/Cargando';
import { EdicionPerfil } from '../componentes/EdicionPerfil';
import { useConsulta } from '../hooks/useConsulta';
import type { UsuarioResponse } from '../modelos/usuario';
import { obtenerMiPerfil } from '../servicios/usuarioServicio';

/**
 * Mi perfil (7.2), para todos los roles: ver los datos de la cuenta, editar el nombre y el teléfono (el correo no se edita)
 * y cambiar la contraseña. Siempre es el perfil de quien tiene la sesión; el backend lo toma del token.
 */
export default function Perfil() {
  const { resultado, cargando } = useConsulta('perfil', (signal) => obtenerMiPerfil(signal));
  // Lo que el backend devolvió al guardar: se muestra sin volver a consultar
  const [actualizado, setActualizado] = useState<UsuarioResponse | null>(null);
  const perfil = actualizado ?? (resultado?.tipo === 'datos' ? resultado.datos : null);

  return (
    <section>
      <h1 className="h3 mb-3">Mi perfil</h1>

      <div aria-live="polite">
        {cargando && <Cargando />}
        {resultado?.tipo === 'error' && <div className="alert alert-danger" role="alert">{resultado.mensaje}</div>}
      </div>

      {perfil && (
        <div className="row g-4">
          <div className="col-12 col-lg-6">
            <div className="card h-100">
              <div className="card-body">
                <h2 className="h5 card-title">Mis datos</h2>
                {/* key: si el servidor devuelve otros datos, el formulario se vuelve a armar con ellos */}
                <EdicionPerfil key={`${perfil.nombre}|${perfil.telefono}`} perfil={perfil} alActualizar={setActualizado} />
                <p className="text-body-secondary small mt-3 mb-0">
                  El nombre que aparece en el encabezado se actualiza la próxima vez que inicie sesión.
                </p>
              </div>
            </div>
          </div>
          <div className="col-12 col-lg-6">
            <div className="card h-100">
              <div className="card-body">
                <h2 className="h5 card-title">Cambiar contraseña</h2>
                <CambioContrasena />
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
