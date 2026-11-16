package co.edu.uniquindio.sga.domain.repository;

import java.util.Optional;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;

/** Puerto de los canales de venta (7.8). Baja lógica: nunca se borra un canal. */
public interface CanalVentaRepository {

    /** Crea o actualiza (identidad por código). */
    CanalVenta guardar(CanalVenta canal);

    Optional<CanalVenta> buscarPorCodigo(String codigo);

    /** El canal cuya credencial tiene ese hash (activo o no): así la autenticación distingue "no existe" de "desactivado". */
    Optional<CanalVenta> buscarPorCredencialHash(String hash);

    /** De 10 en 10, por código. */
    Pagina<CanalVenta> listar(SolicitudPagina pagina);
}
