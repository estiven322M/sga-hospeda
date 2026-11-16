package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ServicioAdicionalPersistenceMapper;

@Repository
public class ServicioAdicionalJpaRepository implements ServicioAdicionalRepository {

    private static final Sort POR_CODIGO = Sort.by("codigo");

    private final ServicioAdicionalJpaDataRepository datos;
    private final ServicioAdicionalPersistenceMapper mapper;

    public ServicioAdicionalJpaRepository(ServicioAdicionalJpaDataRepository datos, ServicioAdicionalPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ServicioAdicional guardar(ServicioAdicional servicio) {
        return mapper.aDominio(datos.save(mapper.aEntidad(servicio)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServicioAdicional> buscarPorCodigo(String codigo) {
        return datos.findById(codigo).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ServicioAdicional> listarTodos(SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(datos.findAll(PaginacionJpa.solicitud(pagina, POR_CODIGO)), pagina, mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ServicioAdicional> listarActivos(SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(datos.findByActivoTrue(PaginacionJpa.solicitud(pagina, POR_CODIGO)), pagina, mapper::aDominio);
    }
}
