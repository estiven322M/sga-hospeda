import { useCallback, useEffect, useRef, useState } from 'react';
import { estadoHttp, fueCancelada, mensajeDeError } from '../utilidades/errores';

export type Resultado<T> =
  | { tipo: 'datos'; datos: T }
  | { tipo: 'error'; mensaje: string; estadoHttp: number | undefined };

interface Guardada<T> {
  clave: string;
  resultado: Resultado<T>;
}

/**
 * Consulta al servidor con AbortController. `clave` identifica la petición (cámbiela cuando cambien sus parámetros):
 * una clave nueva cancela la consulta anterior, y una respuesta que llega tarde nunca pisa a la actual. Con
 * `clave === null` no se consulta. `recargar()` repite la misma petición (tras una acción, por ejemplo).
 *
 * `resultado` es null mientras no haya respuesta para la petición actual: "cargando" se DERIVA, no se guarda en un
 * efecto.
 */
export function useConsulta<T>(clave: string | null, consultar: (signal: AbortSignal) => Promise<T>) {
  const [guardada, setGuardada] = useState<Guardada<T> | null>(null);
  const [version, setVersion] = useState(0);

  // Siempre la última función, sin que un cambio de identidad de `consultar` dispare otra consulta
  const consultarActual = useRef(consultar);
  useEffect(() => {
    consultarActual.current = consultar;
  });

  const claveCompleta = clave === null ? null : `${clave}#${version}`;

  useEffect(() => {
    if (claveCompleta === null) return;
    const controlador = new AbortController();
    consultarActual
      .current(controlador.signal)
      .then((datos) => setGuardada({ clave: claveCompleta, resultado: { tipo: 'datos', datos } }))
      .catch((error: unknown) => {
        if (fueCancelada(error)) return; // la cancelamos nosotros: no es un error que mostrar
        setGuardada({
          clave: claveCompleta,
          resultado: { tipo: 'error', mensaje: mensajeDeError(error), estadoHttp: estadoHttp(error) },
        });
      });
    return () => controlador.abort();
  }, [claveCompleta]);

  const resultado = claveCompleta !== null && guardada?.clave === claveCompleta ? guardada.resultado : null;
  const recargar = useCallback(() => setVersion((anterior) => anterior + 1), []);

  return { resultado, cargando: claveCompleta !== null && resultado === null, recargar };
}
