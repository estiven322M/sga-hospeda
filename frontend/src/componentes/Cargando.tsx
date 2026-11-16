/** Indicador de carga: mientras se descarga una página diferida o se espera la respuesta del servidor. */
export function Cargando() {
  return (
    <div className="d-flex justify-content-center my-5">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Cargando…</span>
      </div>
    </div>
  );
}
