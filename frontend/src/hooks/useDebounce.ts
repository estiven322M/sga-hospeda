import { useEffect, useState } from 'react';

/**
 * Devuelve `valor` con retraso: solo cambia cuando `valor` lleva `milisegundos` sin cambiar. Sirve para no consultar
 * al servidor con cada tecla, sino cuando la persona hace una pausa.
 */
export function useDebounce<T>(valor: T, milisegundos: number): T {
  const [retrasado, setRetrasado] = useState(valor);

  useEffect(() => {
    const temporizador = setTimeout(() => setRetrasado(valor), milisegundos);
    return () => clearTimeout(temporizador); // un valor nuevo cancela la espera del anterior
  }, [valor, milisegundos]);

  return retrasado;
}
