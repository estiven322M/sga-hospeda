import { Link } from 'react-router';
import type { ApartamentoDisponibleResponse } from '../modelos/cotizacion';
import { RUTAS } from '../rutas/rutas';
import { formatearPesos } from '../utilidades/formato';
import { EtiquetasCaracteristicas } from './EtiquetasCaracteristicas';
import { Imagen } from './Imagen';

/**
 * Un apartamento disponible y lo que costaría la estancia buscada. Solo muestra lo que el backend calculó: el total,
 * y si hay descuento por estancia larga (RP-02) el porcentaje y el valor descontado. No recalcula nada.
 */
export function TarjetaApartamento({ apartamento }: { apartamento: ApartamentoDisponibleResponse }) {
  const conDescuento = apartamento.porcentajeDescuento > 0;

  return (
    <article className="card mb-3">
      <div className="card-body d-flex flex-column flex-md-row justify-content-between gap-3">
        {/* 7.5: la tarjeta muestra la imagen principal; si no hay o no carga, un marcador del mismo tamaño */}
        <Imagen
          src={apartamento.imagenPrincipal}
          alt={`Imagen principal de ${apartamento.nombre}`}
          className="rounded object-fit-cover flex-shrink-0"
        />
        <div className="flex-grow-1">
          <h2 className="h5 card-title mb-1">{apartamento.nombre}</h2>
          <p className="text-body-secondary small mb-2">
            Capacidad: {apartamento.capacidad} personas · {apartamento.dormitorios}{' '}
            {apartamento.dormitorios === 1 ? 'dormitorio' : 'dormitorios'}
          </p>
          <EtiquetasCaracteristicas caracteristicas={apartamento.caracteristicas} />
        </div>
        <div className="text-md-end">
          <div className="small text-body-secondary">Total estimado</div>
          <div className="fs-4 fw-semibold">{formatearPesos(apartamento.total)}</div>
          {conDescuento && (
            <div className="small text-success">
              Descuento por estancia larga ({apartamento.porcentajeDescuento} %): −{formatearPesos(apartamento.descuento)}
            </div>
          )}
          <Link to={RUTAS.detalleApartamento(apartamento.identificacion)} className="btn btn-primary btn-sm mt-2">
            Ver detalle
          </Link>
        </div>
      </div>
    </article>
  );
}
