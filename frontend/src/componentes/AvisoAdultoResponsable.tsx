import { useMemo } from 'react';
import { useFormContext, useWatch } from 'react-hook-form';
import { edadALaFecha } from '../esquemas/reserva';
import { EDAD_ADULTO_RESPONSABLE } from '../utilidades/entorno';

interface ConEstanciaYOcupantes {
  fechaEntrada: string;
  ocupantes: { fechaNacimiento: string }[];
}

const FECHA = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Aviso (no bloqueo) de RP-03: si ningún ocupante tendrá la edad de adulto responsable a la fecha de entrada, la
 * reserva será rechazada. El umbral sale de VITE_EDAD_ADULTO_RESPONSABLE solo para avisar; la regla real y quien
 * acepta o rechaza la reserva es el backend. Va dentro de un <FormProvider> con `fechaEntrada` y `ocupantes`.
 */
export function AvisoAdultoResponsable() {
  const { control } = useFormContext<ConEstanciaYOcupantes>();
  const entrada = useWatch({ control, name: 'fechaEntrada' });
  const ocupantes = useWatch({ control, name: 'ocupantes' });

  const edadMinima = EDAD_ADULTO_RESPONSABLE;
  const sinAdulto = useMemo(() => {
    if (edadMinima === null) return false; // sin umbral configurado no se avisa
    const fechas = (ocupantes ?? []).map((ocupante) => ocupante.fechaNacimiento);
    if (!FECHA.test(entrada) || fechas.length === 0 || fechas.some((fecha) => !FECHA.test(fecha))) {
      return false; // sin datos completos no se puede afirmar nada
    }
    return fechas.every((fecha) => edadALaFecha(fecha, entrada) < edadMinima);
  }, [entrada, ocupantes, edadMinima]);

  if (!sinAdulto) return null;
  return (
    <div className="alert alert-warning" role="status">
      Ningún ocupante tendrá {edadMinima} años cumplidos a la fecha de entrada. La reserva exige un adulto
      responsable (RP-03) y el sistema la rechazará si no lo hay.
    </div>
  );
}
