package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.CanalNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;

/** Baja lógica de un canal y su reactivación (7.8): desactivarlo invalida su credencial, pero no borra su historial ni sus reservas. */
@Service
public class CambiarActividadCanalVentaUseCase {

    private final CanalVentaRepository canales;

    public CambiarActividadCanalVentaUseCase(CanalVentaRepository canales) {
        this.canales = canales;
    }

    @Transactional
    public CanalVenta desactivar(String codigo) {
        CanalVenta canal = buscar(codigo);
        canal.desactivar();
        return canales.guardar(canal);
    }

    @Transactional
    public CanalVenta activar(String codigo) {
        CanalVenta canal = buscar(codigo);
        canal.activar();
        return canales.guardar(canal);
    }

    private CanalVenta buscar(String codigo) {
        return canales.buscarPorCodigo(codigo).orElseThrow(() -> new CanalNoEncontradoException(codigo));
    }
}
