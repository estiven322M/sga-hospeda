package co.edu.uniquindio.sga.infrastructure.notificaciones;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/**
 * Bandeja de notificaciones en memoria (10.3.2-A, I-02): conserva los últimos {@value #CAPACIDAD} avisos intentados.
 * Se vacía al reiniciar el servidor y no toca la base de datos. Es segura entre hilos.
 */
@Component
public class BandejaEnMemoria implements BandejaNotificaciones {

    static final int CAPACIDAD = 500;

    // El más reciente va primero: así el listado ya sale en el orden pedido
    private final List<RegistroNotificacion> registros = new ArrayList<>();

    @Override
    public synchronized void registrar(RegistroNotificacion registro) {
        registros.add(0, registro);
        if (registros.size() > CAPACIDAD) {
            registros.subList(CAPACIDAD, registros.size()).clear();
        }
    }

    @Override
    public synchronized Pagina<RegistroNotificacion> listar(SolicitudPagina pagina) {
        return Pagina.de(List.copyOf(registros), pagina);
    }

    /** Solo para las pruebas: vacía la bandeja. */
    public synchronized void vaciar() {
        registros.clear();
    }
}
