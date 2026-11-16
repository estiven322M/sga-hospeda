package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/**
 * Constancia de los avisos intentados (10.3.2-A). Hace visible la integración: en modo local es la "bandeja de
 * salida" con la que se demuestran los envíos sin internet; con el proveedor real deja el resultado de cada envío
 * (sin el contenido de los avisos sensibles). Es en memoria: se vacía al reiniciar.
 */
public interface BandejaNotificaciones {

    void registrar(RegistroNotificacion registro);

    /** De 10 en 10, del más reciente al más antiguo. */
    Pagina<RegistroNotificacion> listar(SolicitudPagina pagina);
}
