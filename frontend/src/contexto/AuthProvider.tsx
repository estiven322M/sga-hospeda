import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router';
import type { LoginRequest, RolUsuario, TokenResponse } from '../modelos/auth';
import { RUTAS } from '../rutas/rutas';
import { registrarManejadores } from '../servicios/api';
import { login } from '../servicios/authServicio';
import { borrarSesion, guardarSesion, obtenerToken, sesionVigente } from '../servicios/sesion';
import { leerToken } from '../utilidades/jwt';
import { notificarAviso } from '../utilidades/notificaciones';
import { ContextoAuth, type Sesion, type ValorAuth } from './contextoAuth';

function aSesion(respuesta: TokenResponse): Sesion {
  const datos = leerToken(respuesta.token);
  return {
    token: respuesta.token,
    // Los roles que dice el backend en la respuesta del login; el token es la misma información firmada.
    roles: respuesta.roles,
    correo: datos?.correo ?? null,
    documento: datos?.documento ?? null,
    expiraEnMs: datos?.expiraEnMs ?? null,
  };
}

function sesionInicial(): Sesion | null {
  const guardada = sesionVigente(); // una sesión con el token vencido no se restaura: se descarta
  return guardada ? aSesion(guardada) : null;
}

/**
 * Dueño de la sesión. Toda la aplicación la lee con useAuth(). Debe estar DENTRO del enrutador (usa useNavigate para
 * llevar al login o a "no autorizado" cuando el backend lo exige).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  // La función se ejecuta una sola vez: lee localStorage al arrancar la aplicación
  const [sesion, setSesion] = useState<Sesion | null>(sesionInicial);

  // useNavigate cambia de identidad con cada navegación; una referencia evita volver a registrar los manejadores.
  const navigate = useNavigate();
  const navegar = useRef(navigate);
  useEffect(() => {
    navegar.current = navigate;
  });

  const iniciarSesion = useCallback(async (credenciales: LoginRequest) => {
    const respuesta = await login(credenciales);
    guardarSesion(respuesta); // para sobrevivir a una recarga de la página
    const nueva = aSesion(respuesta);
    setSesion(nueva); // para que React vuelva a pintar a quien dependa de la sesión
    return nueva;
  }, []);

  const cerrarSesion = useCallback(() => {
    borrarSesion();
    setSesion(null);
  }, []);

  // 401 → sesión vencida o inválida: se cierra, se avisa y se va al login. 403 → no tiene permiso: página aparte.
  useEffect(() => {
    return registrarManejadores({
      alNoAutenticado: () => {
        // Varias peticiones en vuelo pueden fallar a la vez: solo la primera cierra la sesión y avisa.
        if (obtenerToken() === null) return;
        borrarSesion();
        setSesion(null);
        notificarAviso('Su sesión expiró. Ingrese nuevamente.');
        navegar.current(RUTAS.login, { replace: true, state: { desde: window.location.pathname } });
      },
      alProhibido: () => navegar.current(RUTAS.noAutorizado, { replace: true }),
    });
  }, []);

  // El token vence aunque la persona no haga nada: cuando llega su hora se cierra la sesión, sin esperar al 401.
  const expiraEnMs = sesion?.expiraEnMs ?? null;
  useEffect(() => {
    if (expiraEnMs === null) return;
    const temporizador = setTimeout(() => {
      borrarSesion();
      setSesion(null);
      notificarAviso('Su sesión expiró. Ingrese nuevamente.');
      navegar.current(RUTAS.login, { replace: true });
    }, Math.max(expiraEnMs - Date.now(), 0));
    return () => clearTimeout(temporizador);
  }, [expiraEnMs]);

  // useMemo: el objeto solo cambia cuando cambia la sesión. Sin él, cada render del proveedor obligaría a
  // re-pintar a todos los consumidores.
  const valor = useMemo<ValorAuth>(
    () => ({
      sesion,
      autenticado: sesion !== null,
      iniciarSesion,
      cerrarSesion,
      tieneRol: (...roles: RolUsuario[]) => roles.some((rol) => sesion?.roles.includes(rol) ?? false),
    }),
    [sesion, iniciarSesion, cerrarSesion],
  );

  // React 19: el contexto se usa directamente como proveedor (antes, <ContextoAuth.Provider>)
  return <ContextoAuth value={valor}>{children}</ContextoAuth>;
}
