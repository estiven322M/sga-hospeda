package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;

/** Los canales de venta, de 10 en 10 y por código (7.8). */
@Service
public class ListarCanalesVentaUseCase {

    private final CanalVentaRepository canales;

    public ListarCanalesVentaUseCase(CanalVentaRepository canales) {
        this.canales = canales;
    }

    @Transactional(readOnly = true)
    public Pagina<CanalVenta> ejecutar(SolicitudPagina pagina) {
        return canales.listar(pagina);
    }
}
