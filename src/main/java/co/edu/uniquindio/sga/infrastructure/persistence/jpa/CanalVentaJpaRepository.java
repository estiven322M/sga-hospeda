package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CanalVentaEntity;

/** Adaptador JPA del puerto CanalVentaRepository: la entidad de persistencia se mapea al canal del dominio y de vuelta. */
@Repository
public class CanalVentaJpaRepository implements CanalVentaRepository {

    private static final Sort POR_CODIGO = Sort.by("codigo");

    private final CanalVentaJpaDataRepository datos;

    public CanalVentaJpaRepository(CanalVentaJpaDataRepository datos) {
        this.datos = datos;
    }

    @Override
    @Transactional
    public CanalVenta guardar(CanalVenta canal) {
        return aDominio(datos.save(new CanalVentaEntity(canal.getCodigo(), canal.getNombre(), canal.getTipo(),
                canal.getCredencialHash(), canal.isActivo())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CanalVenta> buscarPorCodigo(String codigo) {
        return datos.findById(codigo.trim().toUpperCase()).map(CanalVentaJpaRepository::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CanalVenta> buscarPorCredencialHash(String hash) {
        return datos.findByCredencialHash(hash).map(CanalVentaJpaRepository::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<CanalVenta> listar(SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(datos.findAll(PaginacionJpa.solicitud(pagina, POR_CODIGO)), pagina, CanalVentaJpaRepository::aDominio);
    }

    private static CanalVenta aDominio(CanalVentaEntity e) {
        return CanalVenta.reconstruir(e.getCodigo(), e.getNombre(), e.getTipo(), e.getCredencialHash(), e.isActivo());
    }
}
