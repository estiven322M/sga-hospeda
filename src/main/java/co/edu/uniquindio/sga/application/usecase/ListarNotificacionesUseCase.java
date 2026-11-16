package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/** Bandeja de notificaciones (10.3.2-A): los avisos intentados, de 10 en 10, del más reciente al más antiguo. */
@Service
public class ListarNotificacionesUseCase {

    private final BandejaNotificaciones bandeja;

    public ListarNotificacionesUseCase(BandejaNotificaciones bandeja) {
        this.bandeja = bandeja;
    }

    public Pagina<RegistroNotificacion> ejecutar(SolicitudPagina pagina) {
        return bandeja.listar(pagina);
    }
}
